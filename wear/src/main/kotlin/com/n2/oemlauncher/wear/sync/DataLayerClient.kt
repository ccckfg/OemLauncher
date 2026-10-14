package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset

/** Thin seam over the Wearable Data Layer so the sync logic stays unit-testable. */
interface DataLayerClient {
    suspend fun put(path: String, schema: Int, payload: ByteArray, assets: Map<String, Asset>, urgent: Boolean): Boolean

    suspend fun send(path: String): Boolean

    /**
     * Reads one Asset into memory. Returns null when the link is down, the asset is gone, or
     * the payload is larger than ProtocolLimits.MAX_ICON_BYTES (oversized icons are dropped,
     * never buffered in full).
     */
    suspend fun readAsset(asset: Asset): ByteArray?
}
