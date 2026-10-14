package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.shared.protocol.SyncPaths
import com.n2.oemlauncher.wear.config.WearConstants

class CatalogPublisher(private val client: DataLayerClient) {
    private var lastDigest: String? = null
    private var lastSentAt = 0L

    suspend fun publish(catalog: WatchCatalog, iconBytes: Map<String, ByteArray>, force: Boolean = false, now: Long = System.currentTimeMillis()): Boolean {
        val payload = PayloadCodec.encodeCatalog(catalog)
        val digest = IconId.of(payload)
        if (!force && (digest == lastDigest || (lastDigest != null && now - lastSentAt < WearConstants.CATALOG_THROTTLE_MS))) return false
        val assets = iconBytes.filterKeys { IconId.isValid(it) }.mapValues { Asset.createFromBytes(it.value) }
        val sent = client.put(SyncPaths.CATALOG, ProtocolVersion.CURRENT, payload, assets, urgent = false)
        if (sent) {
            lastDigest = digest
            lastSentAt = now
        }
        return sent
    }
}
