package com.n2.oemlauncher.wear.data

import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState

/**
 * What the watch remembers across restarts. [state] is the full sync state; [config] is the
 * config the launcher currently shows, which the UI still writes directly (see
 * `ConfigRepository.save`), so restore has to reconcile both. [dirty] means the last state
 * receipt was never acknowledged by the phone and must be re-sent.
 */
data class PersistedSyncState(
    val state: WatchConfigState?,
    val config: LauncherConfig?,
    val dirty: Boolean,
)

/** Durable home of the sync state, written in the same transaction as the config. */
interface SyncStateStore {
    suspend fun load(): PersistedSyncState

    /** Persists [state] (including its config) atomically; [dirty] marks an unsent receipt. */
    suspend fun save(state: WatchConfigState, dirty: Boolean)
}
