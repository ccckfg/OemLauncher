package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.shared.protocol.SyncPaths
import com.n2.oemlauncher.wear.config.WearConstants

/** Result of one catalog publish attempt. */
enum class CatalogPublishOutcome {
    /** A put was attempted and the Data Layer accepted it. */
    SENT,

    /** Nothing was sent because the content equals the last acknowledged catalog. */
    SKIPPED_DUPLICATE,

    /** The Data Layer call failed; the caller may retry later. */
    FAILED,
}

/**
 * Sends the catalog DataItem. The content digest ignores `generatedAtMillis`, so a refresh that
 * only moves the timestamp does not re-send the same catalog; the digest is remembered only
 * after a successful put.
 */
class CatalogPublisher(private val client: DataLayerClient) {
    private var lastDigest: String? = null
    private var lastSentAt = 0L

    /** Content digest of [catalog], stable across refreshes that only change the timestamp. */
    fun contentDigest(catalog: WatchCatalog): String =
        IconId.of(PayloadCodec.encodeCatalog(catalog.copy(generatedAtMillis = 0)))

    fun isThrottled(now: Long): Boolean =
        lastSentAt != 0L && now - lastSentAt < WearConstants.CATALOG_THROTTLE_MS

    /** Milliseconds until the throttle window opened by the last send ends; 0 when it is open. */
    fun millisUntilWindowEnd(now: Long): Long =
        if (!isThrottled(now)) 0L else lastSentAt + WearConstants.CATALOG_THROTTLE_MS - now

    suspend fun publish(
        catalog: WatchCatalog,
        iconBytes: Map<String, ByteArray>,
        force: Boolean,
        now: Long,
    ): CatalogPublishOutcome {
        val digest = contentDigest(catalog)
        if (!force && digest == lastDigest) return CatalogPublishOutcome.SKIPPED_DUPLICATE
        val assets = iconBytes
            .filter { (id, bytes) -> IconId.isValid(id) && bytes.isNotEmpty() }
            .mapKeys { (id, _) -> SyncKeys.iconAssetKey(id) }
            .mapValues { (_, bytes) -> Asset.createFromBytes(bytes) }
        val sent = client.put(SyncPaths.CATALOG, ProtocolVersion.CURRENT, PayloadCodec.encodeCatalog(catalog), assets, urgent = false)
        if (!sent) return CatalogPublishOutcome.FAILED
        lastDigest = digest
        lastSentAt = now
        return CatalogPublishOutcome.SENT
    }
}
