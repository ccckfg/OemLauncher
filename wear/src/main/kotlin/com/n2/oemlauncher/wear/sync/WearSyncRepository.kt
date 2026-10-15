package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.ConfigOrigin
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.RevisionPolicy
import com.n2.oemlauncher.shared.protocol.SyncPaths
import com.n2.oemlauncher.wear.data.IconStore
import com.n2.oemlauncher.wear.data.PersistedSyncState
import com.n2.oemlauncher.wear.data.SyncStateStore
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Catalog payload plus the default icons it references, as published to the phone. */
data class CatalogBundle(val catalog: WatchCatalog, val icons: Map<String, ByteArray>)

/** Builds the catalog to publish; the caller supplies the config to render it for. */
fun interface CatalogProvider {
    suspend fun load(config: LauncherConfig): CatalogBundle
}

/** Observable sync status for the launcher UI. */
data class SyncStatus(
    /** null until the first Data Layer attempt of this process; then whether it reached the phone. */
    val connected: Boolean? = null,
    /** Time of the last successful send (state or catalog); 0 means never since this process started. */
    val lastSyncMillis: Long = 0,
    val lastResult: ApplyResult = ApplyResult.NONE,
    val missingIconCount: Int = 0,
)

/**
 * Single writer for everything that crosses the Data Layer. Desired configs, local edits and
 * catalog publishes share one lock and one durable [WatchConfigState], so they can never
 * overwrite each other or roll the revision back (see docs/sync-protocol.md section 3).
 */
class WearSyncRepository(
    private val store: SyncStateStore,
    private val iconStore: IconStore,
    private val client: DataLayerClient,
    private val catalogProvider: CatalogProvider,
    private val scope: CoroutineScope,
    private val onStateChanged: (WatchConfigState) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
    private val assetReader: DesiredAssetReader = DesiredAssetReader(client),
) {
    private val mutex = Mutex()
    private val publisher = CatalogPublisher(client)
    private val processor = DesiredProcessor(iconStore)

    private var restored = false
    private var state = WatchConfigState(LauncherConfig())
    private var statePending = false
    private var lastStateDigest: String? = null

    private var catalogPending = false
    private var catalogFlushJob: Job? = null

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Processes one desired config. [reportedRevision] is the revision read from the DataMap
     * `SyncKeys.REVISION` key; it ties schema/decode/missing-payload rejections to the revision
     * the phone is waiting for. A payload that decodes always reports its own revision.
     */
    suspend fun handleDesired(
        schema: Int,
        payload: ByteArray?,
        assets: Map<String, Asset>,
        reportedRevision: Long? = null,
    ): DesiredOutcome = mutex.withLock {
        ensureRestoredLocked()
        when (val decision = processor.decide(schema, payload, reportedRevision, state, clock())) {
            // A redelivered desired is ignored entirely: no state update, no receipt.
            DesiredDecision.Redelivery -> DesiredOutcome(state, applied = false)
            is DesiredDecision.Rejected -> {
                applyStateLocked(decision.state)
                DesiredOutcome(decision.state, applied = false)
            }
            is DesiredDecision.Apply -> {
                val toFetch = decision.referencedIconIds.filterNot(iconStore::isValidStored)
                val fetched = assetReader.read(assets, toFetch)
                val outcome = processor.apply(decision.desired, fetched, clock())
                applyStateLocked(outcome.state)
                outcome
            }
        }
    }

    /**
     * Applies a local edit inside the sync transaction: reads the durable state, numbers the
     * revision above both the config and the last desired revision, persists, then publishes.
     * The launcher UI should call this instead of saving and publishing separately.
     */
    suspend fun localEdit(transform: (LauncherConfig) -> LauncherConfig): LauncherConfig = mutex.withLock {
        ensureRestoredLocked()
        val now = clock()
        val next = ConfigEditor.stamp(
            transform(state.current),
            RevisionPolicy.next(state.current.revision, state.lastDesiredRevision),
            ConfigOrigin.WATCH,
            now,
        )
        applyStateLocked(state.copy(current = next, updatedAtMillis = now))
        next
    }

    /**
     * Receipt path used by the launcher UI, which stamps and saves the config itself before
     * calling this. An edit based on a stale read is dropped in favour of the newer state.
     */
    suspend fun publishState(config: LauncherConfig) = mutex.withLock {
        ensureRestoredLocked()
        val now = clock()
        if (config.revision >= state.current.revision) {
            applyStateLocked(state.copy(current = config, updatedAtMillis = now))
        } else {
            store.save(state, dirty = statePending)
            publishPendingStateLocked()
        }
    }

    /**
     * Non-forced catalog publish for install/update/uninstall and usage changes. Sends at most
     * once per throttle window; a change inside the window is flushed at the window end, never
     * dropped. Identical content is not re-sent just because the timestamp moved.
     */
    fun requestCatalogPublish() {
        scope.launch {
            mutex.withLock {
                ensureRestoredLocked()
                publishCatalogLocked(force = false)
            }
        }
    }

    /** Forced publish for the request-catalog message: ignores throttle and content dedupe. */
    suspend fun publishCatalogNow() = mutex.withLock {
        ensureRestoredLocked()
        catalogFlushJob?.cancel()
        publishCatalogLocked(force = true)
    }

    /** Re-sends what the phone has not acknowledged, e.g. after a peer reconnects. */
    suspend fun retryPending() = mutex.withLock {
        ensureRestoredLocked()
        publishPendingStateLocked()
        if (catalogPending) publishCatalogLocked(force = false)
    }

    private suspend fun ensureRestoredLocked() {
        if (restored) return
        restored = true
        val persisted = try {
            store.load()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PersistedSyncState(state = null, config = null, dirty = false)
        }
        state = restoreState(persisted)
        statePending = persisted.dirty
        if (persisted.dirty) publishPendingStateLocked()
    }

    private fun restoreState(persisted: PersistedSyncState): WatchConfigState {
        val saved = persisted.state
            ?: persisted.config?.let { WatchConfigState(current = it, lastDesiredRevision = it.revision) }
            ?: return WatchConfigState(LauncherConfig())
        val config = persisted.config
        return if (config != null && config.revision > saved.current.revision) {
            // The UI writes the config key on its own; never restore a state older than the
            // config on screen, or a redelivered desired could roll the launcher back.
            saved.copy(current = config, lastDesiredRevision = maxOf(saved.lastDesiredRevision, config.revision))
        } else {
            saved
        }
    }

    private suspend fun applyStateLocked(next: WatchConfigState) {
        // Persist before touching the in-memory state: if the write fails, the next attempt
        // (redelivered desired or a new local edit) starts from the old, consistent state.
        store.save(next, dirty = true)
        state = next
        _status.update { it.copy(lastResult = next.lastResult, missingIconCount = next.missingIconIds.size) }
        statePending = true
        publishPendingStateLocked()
        // The tile refresh talks to the system service; keep it off the caller's thread.
        scope.launch { onStateChanged(next) }
    }

    private suspend fun publishPendingStateLocked() {
        if (!statePending) return
        val payload = PayloadCodec.encodeState(state)
        val digest = IconId.of(payload)
        if (digest == lastStateDigest) {
            statePending = false
            store.save(state, dirty = false)
            return
        }
        val sent = client.put(SyncPaths.CONFIG_STATE, ProtocolVersion.CURRENT, payload, emptyMap(), urgent = false)
        if (sent) {
            lastStateDigest = digest
            statePending = false
            store.save(state, dirty = false)
        }
        recordTransport(sent)
    }

    private suspend fun publishCatalogLocked(force: Boolean) {
        if (!force && publisher.isThrottled(clock())) {
            catalogPending = true
            scheduleCatalogFlushLocked()
            return
        }
        catalogPending = false
        val bundle = catalogProvider.load(state.current)
        when (publisher.publish(bundle.catalog, bundle.icons, force, clock())) {
            CatalogPublishOutcome.SENT -> recordTransport(sent = true)
            CatalogPublishOutcome.FAILED -> {
                // Keep the change pending so a reconnect resends it; the failed attempt does not
                // open a throttle window, so the next trigger goes out immediately.
                catalogPending = true
                recordTransport(sent = false)
            }
            CatalogPublishOutcome.SKIPPED_DUPLICATE -> Unit
        }
    }

    private fun scheduleCatalogFlushLocked() {
        if (catalogFlushJob?.isActive == true) return
        val wait = publisher.millisUntilWindowEnd(clock())
        catalogFlushJob = scope.launch {
            delay(wait)
            mutex.withLock { if (catalogPending) publishCatalogLocked(force = false) }
        }
    }

    private fun recordTransport(sent: Boolean) {
        _status.update {
            if (sent) it.copy(connected = true, lastSyncMillis = clock()) else it.copy(connected = false)
        }
    }
}
