package com.n2.oemlauncher.mobile.data

import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.n2.oemlauncher.mobile.config.StorageConstants
import com.n2.oemlauncher.mobile.editor.DraftHistory
import com.n2.oemlauncher.mobile.sync.WearGateway
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.config.FavoriteToggle
import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchConfigState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CompanionRepository(
    private val store: SnapshotStore,
    val icons: IconStore,
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(CompanionState())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()

    init {
        scope.launch {
            try {
                val snapshot = store.load()
                mutable.update { it.copy(snapshot = snapshot, loading = false) }
            } catch (_: java.io.IOException) {
                mutable.update { it.copy(loading = false, notice = UserNotice.DATA_INVALID) }
            } finally {
                ready.complete(Unit)
            }
        }
    }

    private suspend fun update(transform: (CompanionSnapshot) -> CompanionSnapshot) {
        ready.await()
        mutex.withLock {
            val snapshot = transform(mutable.value.snapshot)
            mutable.update { it.copy(snapshot = snapshot) }
            try {
                store.save(snapshot)
            } catch (_: java.io.IOException) {
                notice(UserNotice.DATA_INVALID)
            }
        }
    }

    private fun diagnose(transform: (SyncDiagnostics) -> SyncDiagnostics) {
        mutable.update { it.copy(diagnostics = transform(it.diagnostics)) }
    }

    suspend fun edit(transform: (LauncherConfig) -> LauncherConfig) =
        update {
            val draft = it.draft ?: if (it.catalog != null) DraftHistory(LauncherConfig()) else null
            it.copy(draft = draft?.edit(transform))
        }

    suspend fun undo() = update { it.copy(draft = it.draft?.undo()) }
    suspend fun redo() = update { it.copy(draft = it.draft?.redo()) }
    suspend fun toggleFavorite(key: String) = edit {
        val (config, result) = ConfigEditor.toggleFavorite(it, key)
        if (result == FavoriteToggle.LIMIT_REACHED) notice(UserNotice.FAVORITE_LIMIT)
        config
    }

    /** Snapshot once the initial load from storage has finished. */
    suspend fun loaded(): CompanionSnapshot {
        ready.await()
        return mutable.value.snapshot
    }

    /** Swap in regenerated demo apps; their keys are stable, so demo edits survive. */
    suspend fun relabelDemo(catalog: WatchCatalog) = update {
        if (it.demo) it.copy(catalog = catalog) else it
    }

    suspend fun demo(catalog: WatchCatalog, config: LauncherConfig) = update {
        if (it.catalog != null && !it.demo) it else CompanionSnapshot(
            catalog = catalog, draft = DraftHistory(config), demo = true,
        )
    }

    suspend fun receiveCatalog(catalog: WatchCatalog) = update {
        it.copy(catalog = catalog, demo = false, draft = if (it.demo) null else it.draft)
    }.also {
        diagnose { d -> d.copy(lastCatalogMillis = System.currentTimeMillis()) }
    }

    /** Returns true when [watch] is at least as new as [previous]; ties fall to updatedAtMillis. */
    private fun isNotStale(watch: WatchConfigState, previous: WatchConfigState): Boolean {
        if (watch.current.revision != previous.current.revision) {
            return watch.current.revision > previous.current.revision
        }
        if (watch.lastDesiredRevision != previous.lastDesiredRevision) {
            return watch.lastDesiredRevision > previous.lastDesiredRevision
        }
        return watch.updatedAtMillis >= previous.updatedAtMillis
    }

    suspend fun receiveState(watch: WatchConfigState) = update { old ->
        val previous = old.watch
        if (previous != null && !isNotStale(watch, previous)) return@update old
        val applied = watch.lastResult in setOf(ApplyResult.APPLIED, ApplyResult.APPLIED_MISSING_ICONS)
        val sent = old.sent
        val acknowledged = applied && sent != null && sent.revision == watch.lastDesiredRevision
        val draft = when {
            old.draft == null || old.demo -> DraftHistory(watch.current)
            acknowledged -> old.draft.acknowledge(requireNotNull(sent), watch.current.revision)
            else -> old.draft
        }
        old.copy(
            watch = watch, draft = draft, demo = false,
            publishedRevision = if (acknowledged) sent.revision else old.publishedRevision,
            lastSyncMillis = if (acknowledged) watch.updatedAtMillis else old.lastSyncMillis,
        )
    }.also {
        diagnose { d -> d.copy(lastStateMillis = System.currentTimeMillis()) }
    }

    suspend fun loadWatch() = update {
        it.watch?.let { watch -> it.copy(draft = DraftHistory(watch.current)) } ?: it
    }

    suspend fun sync(gateway: WearGateway, overwrite: Boolean = false) {
        ready.await()
        if (!state.value.canSync || (state.value.conflict && !overwrite)) return
        mutable.update { it.copy(busy = true) }
        var outgoing: LauncherConfig? = null
        try {
            // Persist the outbox before publishing: a fast receipt must never beat sentRevision.
            update {
                outgoing = it.draft?.commit(it.watch, it.sent?.revision, System.currentTimeMillis())
                it.copy(sent = outgoing)
            }
            val config = outgoing ?: return
            // A missing local icon file is a data problem, not a connection problem: skip it,
            // let the watch answer APPLIED_MISSING_ICONS, and surface the ids in diagnostics.
            val assets = LinkedHashMap<String, ByteArray>()
            val missing = mutableListOf<String>()
            for (id in config.iconOverrides.values.toSet()) {
                val bytes = icons.bytes(id)
                if (bytes == null) missing += id else assets[id] = bytes
            }
            gateway.publish(config, assets)
            update { it.copy(publishedRevision = config.revision) }
            diagnose { d ->
                d.copy(
                    lastSentRevision = config.revision, lastSendSucceeded = true,
                    localMissingIcons = missing,
                )
            }
            if (missing.isNotEmpty()) notice(UserNotice.ICONS_MISSING)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            diagnose { d ->
                d.copy(
                    lastSentRevision = outgoing?.revision ?: d.lastSentRevision,
                    lastSendSucceeded = false,
                    lastError = ApiError((e as? ApiException)?.statusCode, System.currentTimeMillis()),
                )
            }
            connection(WatchConnection())
            notice(UserNotice.SYNC_FAILED)
        } finally {
            mutable.update { it.copy(busy = false) }
        }
    }

    fun connection(connection: WatchConnection) {
        mutable.update { it.copy(connection = connection) }
        diagnose { it.copy(nodes = connection.nodes) }
    }
    fun notice(notice: UserNotice) {
        mutable.update { it.copy(notice = notice) }
    }
    fun iconsChanged() {
        mutable.update { it.copy(iconGeneration = it.iconGeneration + 1) }
    }
    /** [statusCode] is the ApiException code, or null for non-API failures. */
    fun apiError(statusCode: Int?) {
        diagnose { it.copy(lastError = ApiError(statusCode, System.currentTimeMillis())) }
    }
    fun invalidPayload() {
        Log.w(StorageConstants.TAG, "Rejected wearable payload")
        notice(UserNotice.DATA_INVALID)
    }
}
