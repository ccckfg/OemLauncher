package com.n2.oemlauncher.mobile.sync

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.n2.oemlauncher.mobile.config.SyncLimits
import com.n2.oemlauncher.mobile.data.CompanionRepository
import com.n2.oemlauncher.mobile.data.WatchConnection
import com.n2.oemlauncher.mobile.iconpack.readBounded
import com.n2.oemlauncher.shared.protocol.DecodeResult
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.shared.protocol.SyncPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WearDataReceiver(context: Context, private val repository: CompanionRepository) {
    private val data = Wearable.getDataClient(context)

    suspend fun receive(item: DataItem) = withContext(Dispatchers.IO) {
        val path = item.uri.path
        if (path != SyncPaths.CATALOG && path != SyncPaths.CONFIG_STATE) return@withContext
        try {
            val map = DataMapItem.fromDataItem(item).dataMap
            val schema = map.getInt(SyncKeys.SCHEMA)
            val payload = map.getByteArray(SyncKeys.PAYLOAD) ?: return@withContext
            if (path == SyncPaths.CATALOG) {
                when (val result = PayloadCodec.decodeCatalog(schema, payload)) {
                    is DecodeResult.Failure -> repository.invalidPayload()
                    is DecodeResult.Success -> {
                        fetchIcons(map, result.value.entries.mapNotNull { it.iconId }.toSet())
                        repository.iconsChanged()
                        repository.receiveCatalog(result.value)
                    }
                }
            } else {
                when (val result = PayloadCodec.decodeState(schema, payload)) {
                    is DecodeResult.Failure -> repository.invalidPayload()
                    is DecodeResult.Success -> repository.receiveState(result.value)
                }
            }
        } catch (e: ApiException) {
            repository.apiError(e.statusCode)
            repository.connection(WatchConnection())
        } catch (_: java.io.IOException) {
            repository.apiError(null)
            repository.invalidPayload()
        } catch (_: IllegalArgumentException) {
            repository.invalidPayload()
        }
    }

    /** Download catalog icon assets with bounded parallelism so one catalog cannot monopolize IO. */
    private suspend fun fetchIcons(map: DataMap, ids: Set<String>) = coroutineScope {
        val semaphore = Semaphore(SyncLimits.ASSET_FETCH_CONCURRENCY)
        ids.map { id ->
            async { semaphore.withPermit { fetchIcon(map, id) } }
        }.awaitAll()
        Unit
    }

    private suspend fun fetchIcon(map: DataMap, id: String) {
        val asset = map.getAsset(SyncKeys.iconAssetKey(id)) ?: return
        val response = data.getFdForAsset(asset).await()
        try {
            val bytes = response.inputStream.use { it.readBounded(ProtocolLimits.MAX_ICON_BYTES) }
            if (bytes.size <= ProtocolLimits.MAX_ICON_BYTES && IconId.of(bytes) == id) {
                repository.icons.put(bytes, id)
            }
        } finally {
            response.release()
        }
    }
}
