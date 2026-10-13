package com.n2.oemlauncher.mobile.data

import android.util.Log
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

    suspend fun demo(catalog: WatchCatalog, config: LauncherConfig) = update {
        if (it.catalog != null && !it.demo) it else CompanionSnapshot(
            catalog = catalog, draft = DraftHistory(config), demo = true,
        )
    }

    suspend fun receiveCatalog(catalog: WatchCatalog) = update {
        it.copy(catalog = catalog, demo = false, draft = if (it.demo) null else it.draft)
    }

    suspend fun receiveState(watch: WatchConfigState) = update { old ->
        val previous = old.watch
        if (previous != null && (
            watch.current.revision < previous.current.revision ||
                (watch.current.revision == previous.current.revision &&
                    watch.lastDesiredRevision < previous.lastDesiredRevision)
            )
        ) return@update old
        val applied = watch.lastResult in setOf(ApplyResult.APPLIED, ApplyResult.APPLIED_MISSING_ICONS)
        val acknowledged = applied && old.sent?.revision == watch.lastDesiredRevision
        val draft = when {
            old.draft == null || old.demo -> DraftHistory(watch.current)
            acknowledged -> old.draft.acknowledge(requireNotNull(old.sent), watch.current.revision)
            else -> old.draft
        }
        old.copy(
            watch = watch, draft = draft, demo = false,
            publishedRevision = if (acknowledged) old.sent?.revision else old.publishedRevision,
            lastSyncMillis = if (acknowledged) watch.updatedAtMillis else old.lastSyncMillis,
        )
    }

    suspend fun loadWatch() = update {
        it.watch?.let { watch -> it.copy(draft = DraftHistory(watch.current)) } ?: it
    }

    suspend fun sync(gateway: WearGateway, overwrite: Boolean = false) {
        ready.await()
        if (!state.value.canSync || (state.value.conflict && !overwrite)) return
        mutable.update { it.copy(busy = true) }
        try {
            // Persist the outbox before publishing: a fast receipt must never beat sentRevision.
            var outgoing: LauncherConfig? = null
            update {
                outgoing = it.draft?.commit(it.watch, it.sent?.revision, System.currentTimeMillis())
                it.copy(sent = outgoing)
            }
            val config = outgoing ?: return
            val assets = LinkedHashMap<String, ByteArray>()
            for (id in config.iconOverrides.values.toSet()) {
                assets[id] = requireNotNull(icons.bytes(id))
            }
            gateway.publish(config, assets)
            update { it.copy(publishedRevision = config.revision) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            connection(WatchConnection())
            notice(UserNotice.SYNC_FAILED)
        } finally {
            mutable.update { it.copy(busy = false) }
        }
    }

    fun connection(connection: WatchConnection) {
        mutable.update { it.copy(connection = connection) }
    }
    fun notice(notice: UserNotice) {
        mutable.update { it.copy(notice = notice) }
    }
    fun iconsChanged() {
        mutable.update { it.copy(iconGeneration = it.iconGeneration + 1) }
    }
    fun invalidPayload() {
        Log.w(StorageConstants.TAG, "Rejected wearable payload")
        notice(UserNotice.DATA_INVALID)
    }
}
