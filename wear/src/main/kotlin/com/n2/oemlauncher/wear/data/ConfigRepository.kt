package com.n2.oemlauncher.wear.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.n2.oemlauncher.shared.config.ConfigSanitizer
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.wear.config.WearConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.launcherDataStore by preferencesDataStore("launcher")

/**
 * Everything the watch persists about the launcher: the config the UI shows, the usage
 * counters, and the full sync state (see [SyncStateStore]).
 *
 * The config and the sync state are written in one DataStore transaction, so a restart can
 * never see a state that disagrees with the config it was saved with.
 */
class ConfigRepository(private val context: Context) : SyncStateStore {
    private val configKey = stringPreferencesKey(WearConstants.CONFIG_KEY)
    private val usageKey = stringPreferencesKey(WearConstants.USAGE_KEY)
    private val stateKey = stringPreferencesKey(WearConstants.STATE_KEY)
    private val stateDirtyKey = booleanPreferencesKey(WearConstants.STATE_DIRTY_KEY)
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; coerceInputValues = true }

    val config: Flow<LauncherConfig> = context.launcherDataStore.data.map { prefs ->
        prefs[configKey]?.let(PayloadCodec::configFromJson) ?: LauncherConfig()
    }
    val usage: Flow<UsageStats> = context.launcherDataStore.data.map { prefs ->
        prefs[usageKey]?.let { runCatching { json.decodeFromString<UsageStats>(it) }.getOrNull() }
            ?: emptyMap()
    }

    suspend fun save(config: LauncherConfig) {
        context.launcherDataStore.edit { it[configKey] = PayloadCodec.configToJson(config) }
    }

    /** Records one launch and returns the new total count for that app. */
    suspend fun recordLaunch(key: String, now: Long): Int {
        var count = 0
        context.launcherDataStore.edit { prefs ->
            val old = prefs[usageKey]?.let { runCatching { json.decodeFromString<UsageStats>(it) }.getOrNull() }
                ?: emptyMap()
            val record = old[key] ?: UsageRecord()
            count = record.launchCount + 1
            prefs[usageKey] = json.encodeToString(old + (key to record.copy(
                launchCount = count,
                lastLaunchedMillis = now,
            )))
        }
        return count
    }

    override suspend fun load(): PersistedSyncState {
        val prefs = context.launcherDataStore.data.first()
        return PersistedSyncState(
            state = prefs[stateKey]?.let(::decodeState),
            config = prefs[configKey]?.let(PayloadCodec::configFromJson),
            dirty = prefs[stateDirtyKey] ?: false,
        )
    }

    override suspend fun save(state: WatchConfigState, dirty: Boolean) {
        val config = PayloadCodec.configToJson(state.current)
        val encoded = json.encodeToString(WatchConfigState.serializer(), state)
        context.launcherDataStore.edit { prefs ->
            prefs[configKey] = config
            prefs[stateKey] = encoded
            prefs[stateDirtyKey] = dirty
        }
    }

    private fun decodeState(text: String): WatchConfigState? = try {
        json.decodeFromString(WatchConfigState.serializer(), text).let { state ->
            state.copy(
                current = ConfigSanitizer.sanitize(state.current),
                missingIconIds = state.missingIconIds.filter(IconId::isValid),
            )
        }
    } catch (_: IllegalArgumentException) {
        null
    }
}
