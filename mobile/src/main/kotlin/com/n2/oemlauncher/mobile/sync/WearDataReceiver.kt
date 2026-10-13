package com.n2.oemlauncher.mobile.sync

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
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
                        for (id in result.value.entries.mapNotNull { it.iconId }.toSet()) {
                            val asset = map.getAsset(SyncKeys.iconAssetKey(id)) ?: continue
                            val response = data.getFdForAsset(asset).await()
                            try {
                                val bytes = response.inputStream.use {
                                    it.readBounded(ProtocolLimits.MAX_ICON_BYTES)
                                }
                                if (bytes.size <= ProtocolLimits.MAX_ICON_BYTES && IconId.of(bytes) == id) {
                                    repository.icons.put(bytes, id)
                                }
                            } finally {
                                response.release()
                            }
                        }
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
        } catch (_: ApiException) {
            repository.connection(WatchConnection())
        } catch (_: java.io.IOException) {
            repository.invalidPayload()
        } catch (_: IllegalArgumentException) {
            repository.invalidPayload()
        }
    }
}
