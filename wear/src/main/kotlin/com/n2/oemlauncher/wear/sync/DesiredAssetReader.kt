package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.wear.config.WearConstants
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Downloads the icon assets a desired config references. Reads run on IO with a bounded number
 * of parallel transfers; assets that are absent, fail to download or exceed the protocol size
 * limit are simply missing from the result, and the processor turns them into missingIconIds.
 */
class DesiredAssetReader(
    private val client: DataLayerClient,
    private val maxConcurrency: Int = WearConstants.ICON_FETCH_CONCURRENCY,
) {
    suspend fun read(assets: Map<String, Asset>, iconIds: Collection<String>): Map<String, ByteArray> =
        coroutineScope {
            val gate = Semaphore(maxConcurrency.coerceAtLeast(1))
            iconIds.distinct()
                .mapNotNull { id -> assets[id]?.let { asset -> async(Dispatchers.IO) { readOne(gate, id, asset) } } }
                .awaitAll()
                .filterNotNull()
                .toMap()
        }

    private suspend fun readOne(gate: Semaphore, id: String, asset: Asset): Pair<String, ByteArray>? =
        gate.withPermit {
            try {
                client.readAsset(asset)?.let { id to it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
}
