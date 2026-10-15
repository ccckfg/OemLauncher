package com.n2.oemlauncher.mobile.sync

import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.shared.protocol.SyncPaths

/**
 * Builds the phone-owned desired DataItem. Kept out of [GoogleWearGateway] so the exact set of
 * keys (schema, payload, revision, icon assets) is unit-testable without Play services.
 */
object DesiredItem {
    fun request(config: LauncherConfig, icons: Map<String, ByteArray>): PutDataMapRequest {
        val payload = PayloadCodec.encodeConfig(config)
        require(PayloadCodec.fitsDataItem(payload))
        val request = PutDataMapRequest.create(SyncPaths.CONFIG_DESIRED)
        val map = request.dataMap
        map.putInt(SyncKeys.SCHEMA, ProtocolVersion.CURRENT)
        map.putByteArray(SyncKeys.PAYLOAD, payload)
        // The watch reads this when the payload cannot be decoded, so the revision lives
        // outside the payload as well.
        map.putLong(SyncKeys.REVISION, config.revision)
        for ((id, bytes) in icons) {
            map.putAsset(SyncKeys.iconAssetKey(id), Asset.createFromBytes(bytes))
        }
        return request
    }
}
