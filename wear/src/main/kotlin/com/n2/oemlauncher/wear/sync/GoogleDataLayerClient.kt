package com.n2.oemlauncher.wear.sync

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.n2.oemlauncher.shared.protocol.SyncKeys
import kotlinx.coroutines.tasks.await

class GoogleDataLayerClient(context: Context) : DataLayerClient {
    private val appContext = context.applicationContext
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    override suspend fun put(path: String, schema: Int, payload: ByteArray, assets: Map<String, Asset>, urgent: Boolean): Boolean =
        try {
            val request = PutDataMapRequest.create(path).apply {
                dataMap.putInt(SyncKeys.SCHEMA, schema)
                dataMap.putByteArray(SyncKeys.PAYLOAD, payload)
                assets.forEach { (key, asset) -> dataMap.putAsset(key, asset) }
            }.asPutDataRequest().setUrgentIf(urgent)
            dataClient.putDataItem(request).await()
            true
        } catch (_: ApiException) {
            false
        } catch (_: RuntimeException) {
            false
        }

    override suspend fun send(path: String): Boolean = try {
        val nodes = Wearable.getNodeClient(appContext).connectedNodes.await()
        nodes.forEach { messageClient.sendMessage(it.id, path, ByteArray(0)).await() }
        nodes.isNotEmpty()
    } catch (_: Throwable) {
        false
    }

    private fun com.google.android.gms.wearable.PutDataRequest.setUrgentIf(urgent: Boolean) =
        apply { if (urgent) setUrgent() }
}
