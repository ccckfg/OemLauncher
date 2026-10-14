package com.n2.oemlauncher.wear.sync

import android.net.Uri
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.shared.protocol.SyncPaths
import com.n2.oemlauncher.wear.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class WearSyncService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(buffer: DataEventBuffer) {
        buffer.use { events -> events.forEach { event -> if (event.type == DataEvent.TYPE_CHANGED) handle(event) } }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == SyncPaths.MESSAGE_REQUEST_CATALOG) {
            scope.launch { publishCatalog(force = true) }
        }
    }

    private fun handle(event: DataEvent) {
        if (event.dataItem.uri.path != SyncPaths.CONFIG_DESIRED) return
        val map = DataMapItem.fromDataItem(event.dataItem).dataMap
        val schema = map.getInt(SyncKeys.SCHEMA, -1)
        val payload = map.getByteArray(SyncKeys.PAYLOAD) ?: return
        scope.launch {
            // Asset descriptors are handed to the Data Layer adapter in production. The
            // processor itself receives decoded bytes, which keeps validation testable on JVM.
            (application as App).container.syncRepository.handleDesired(schema, payload, emptyMap())
        }
    }

    private suspend fun publishCatalog(force: Boolean) {
        val container = (application as App).container
        val config = container.configRepository.config.first()
        val usage = container.configRepository.usage.first()
        val catalog = container.catalogRepository.refresh(config, usage)
        val icons = catalog.entries.mapNotNull { entry ->
            val id = entry.iconId ?: return@mapNotNull null
            container.iconStore.get(id)?.let { id to it }
        }.toMap()
        container.syncRepository.publishCatalog(catalog, icons, force)
    }
}
