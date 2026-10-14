package com.n2.oemlauncher.wear.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.model.ConfigOrigin
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.protocol.RevisionPolicy
import com.n2.oemlauncher.wear.config.WearConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.launcherDataStore by preferencesDataStore("launcher")

interface ConfigWriter {
    suspend fun save(config: LauncherConfig)
}

class ConfigRepository(private val context: Context) : ConfigWriter {
    private val configKey = stringPreferencesKey(WearConstants.CONFIG_KEY)
    private val usageKey = stringPreferencesKey(WearConstants.USAGE_KEY)
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    val config: Flow<LauncherConfig> = context.launcherDataStore.data.map { prefs ->
        prefs[configKey]?.let(PayloadCodec::configFromJson) ?: LauncherConfig()
    }
    val usage: Flow<UsageStats> = context.launcherDataStore.data.map { prefs ->
        prefs[usageKey]?.let { runCatching { json.decodeFromString<UsageStats>(it) }.getOrNull() }
            ?: emptyMap()
    }

    override suspend fun save(config: LauncherConfig) {
        context.launcherDataStore.edit { it[configKey] = PayloadCodec.configToJson(config) }
    }

    suspend fun edit(current: LauncherConfig, transform: (LauncherConfig) -> LauncherConfig, now: Long) {
        val edited = transform(current)
        save(ConfigEditor.stamp(edited, RevisionPolicy.next(current.revision), ConfigOrigin.WATCH, now))
    }

    suspend fun recordLaunch(key: String, now: Long) {
        context.launcherDataStore.edit { prefs ->
            val old = prefs[usageKey]?.let { runCatching { json.decodeFromString<UsageStats>(it) }.getOrNull() }
                ?: emptyMap()
            val record = old[key] ?: UsageRecord()
            prefs[usageKey] = json.encodeToString(old + (key to record.copy(
                launchCount = record.launchCount + 1,
                lastLaunchedMillis = now,
            )))
        }
    }
}
