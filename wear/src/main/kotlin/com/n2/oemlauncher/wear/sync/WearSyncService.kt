package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.shared.protocol.SyncPaths
import com.n2.oemlauncher.wear.App
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** One desired config as read off the Data Layer, with its icon assets keyed by icon id. */
private class DesiredMessage(
    val schema: Int,
    /** Null when the DataMap has no payload; the receipt is still published as invalid. */
    val payload: ByteArray?,
    /** `SyncKeys.REVISION`, the revision the phone is waiting for; optional. */
    val revision: Long?,
    val assets: Map<String, Asset>,
)

class WearSyncService : WearableListenerService() {
    // Cancelled in onDestroy. Work that must survive is durable elsewhere: unprocessed DataItems
    // are redelivered by the Data Layer on the next start, and receipts stay dirty until sent.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val container get() = (application as App).container

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onDataChanged(buffer: DataEventBuffer) {
        buffer.use { events ->
            for (event in events) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                if (event.dataItem.uri.path != SyncPaths.CONFIG_DESIRED) continue
                val desired = readDesired(event.dataItem)
                launchSafely {
                    container.syncRepository.handleDesired(
                        desired.schema,
                        desired.payload,
                        desired.assets,
                        desired.revision,
                    )
                }
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != SyncPaths.MESSAGE_REQUEST_CATALOG) return
        launchSafely { container.syncRepository.publishCatalogNow() }
    }

    override fun onPeerConnected(peer: Node) {
        launchSafely { container.syncRepository.retryPending() }
    }

    /**
     * The DataMap is parsed while the buffer is still open; asset bytes are only fetched later,
     * inside the coroutine, so the callback never blocks on the network. A missing payload is
     * still returned so the rejection receipt can carry the reported revision.
     */
    private fun readDesired(dataItem: DataItem): DesiredMessage {
        val map: DataMap = DataMapItem.fromDataItem(dataItem).dataMap
        val revision = if (map.containsKey(SyncKeys.REVISION)) map.getLong(SyncKeys.REVISION) else null
        val assets = buildMap {
            for (key in map.keySet()) {
                val iconId = SyncKeys.iconIdFromAssetKey(key) ?: continue
                map.getAsset(key)?.let { put(iconId, it) }
            }
        }
        return DesiredMessage(map.getInt(SyncKeys.SCHEMA, -1), map.getByteArray(SyncKeys.PAYLOAD), revision, assets)
    }

    private fun launchSafely(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // No Play services / not paired: the repository already degrades to "offline";
                // anything else is retried through Data Layer redelivery.
            }
        }
    }
}
