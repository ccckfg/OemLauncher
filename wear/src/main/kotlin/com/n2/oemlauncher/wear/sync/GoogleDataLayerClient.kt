package com.n2.oemlauncher.wear.sync

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import com.n2.oemlauncher.shared.protocol.SyncKeys
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: ApiException) {
            false
        } catch (_: RuntimeException) {
            false
        }

    override suspend fun readAsset(asset: Asset): ByteArray? = try {
        val response = dataClient.getFdForAsset(asset).await()
        try {
            response.inputStream?.use { readCapped(it, ProtocolLimits.MAX_ICON_BYTES) }
        } finally {
            runCatching { response.release() }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    override suspend fun send(path: String): Boolean = try {
        val nodes = Wearable.getNodeClient(appContext).connectedNodes.await()
        nodes.forEach { messageClient.sendMessage(it.id, path, ByteArray(0)).await() }
        nodes.isNotEmpty()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private fun com.google.android.gms.wearable.PutDataRequest.setUrgentIf(urgent: Boolean) =
        apply { if (urgent) setUrgent() }
}

/** Reads at most [limit] bytes; a longer stream is dropped instead of being buffered. */
internal fun readCapped(stream: InputStream, limit: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) return output.toByteArray()
        total += read
        if (total > limit) return null
        output.write(buffer, 0, read)
    }
}
