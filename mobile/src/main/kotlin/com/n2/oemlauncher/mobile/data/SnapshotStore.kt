package com.n2.oemlauncher.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.n2.oemlauncher.mobile.config.StorageConstants
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString

interface SnapshotStore {
    suspend fun load(): CompanionSnapshot
    suspend fun save(snapshot: CompanionSnapshot)
}

class PreferencesSnapshotStore(context: Context, scope: CoroutineScope) : SnapshotStore {
    private val key = stringPreferencesKey(StorageConstants.SNAPSHOT_KEY)
    private val store = PreferenceDataStoreFactory.create(scope = scope) {
        context.preferencesDataStoreFile(StorageConstants.DATASTORE_FILE)
    }
    override suspend fun load(): CompanionSnapshot {
        val text = store.data.first()[key] ?: return CompanionSnapshot()
        return try {
            PayloadCodec.json.decodeFromString<CompanionSnapshot>(text)
        } catch (_: IllegalArgumentException) {
            CompanionSnapshot()
        }
    }
    override suspend fun save(snapshot: CompanionSnapshot) = withContext(Dispatchers.IO) {
        val text = PayloadCodec.json.encodeToString(snapshot)
        store.edit { it[key] = text }
        Unit
    }
}
