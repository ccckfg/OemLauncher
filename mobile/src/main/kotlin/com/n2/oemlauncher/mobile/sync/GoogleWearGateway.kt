package com.n2.oemlauncher.mobile.sync

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.n2.oemlauncher.mobile.data.ConnectionKind
import com.n2.oemlauncher.mobile.data.NodeStatus
import com.n2.oemlauncher.mobile.data.WatchConnection
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.protocol.Capabilities
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.shared.protocol.SyncPaths
import kotlinx.coroutines.tasks.await

class GoogleWearGateway(
    private val context: Context,
    private val receiver: WearDataReceiver,
    private val onError: (String) -> Unit = {},
) : WearGateway {
    private val capability = Wearable.getCapabilityClient(context)
    private val nodes = Wearable.getNodeClient(context)
    private val data = Wearable.getDataClient(context)
    private var listener: CapabilityClient.OnCapabilityChangedListener? = null

    override suspend fun discover(): WatchConnection {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) !=
            ConnectionResult.SUCCESS
        ) return WatchConnection(ConnectionKind.PLAY_SERVICES_MISSING)
        return try {
            val installed = capability.getCapability(
                Capabilities.WATCH_APP, CapabilityClient.FILTER_REACHABLE,
            ).await().nodes
            val connected = nodes.connectedNodes.await()
            val all = (connected.map { node ->
                NodeStatus(node.displayName, installed.any { it.id == node.id })
            } + installed.filter { app -> connected.none { it.id == app.id } }.map {
                NodeStatus(it.displayName, hasWatchApp = true)
            }).distinctBy { it.name }
            when {
                installed.isNotEmpty() -> WatchConnection(ConnectionKind.CONNECTED, all)
                connected.isNotEmpty() -> WatchConnection(ConnectionKind.APP_MISSING, all)
                else -> WatchConnection()
            }
        } catch (_: ApiException) {
            onError(API_EXCEPTION)
            WatchConnection()
        }
    }

    override suspend fun requestCatalog() {
        try {
            val installed = capability.getCapability(
                Capabilities.WATCH_APP, CapabilityClient.FILTER_REACHABLE,
            ).await().nodes
            for (node in installed) {
                Wearable.getMessageClient(context).sendMessage(
                    node.id, SyncPaths.MESSAGE_REQUEST_CATALOG, byteArrayOf(),
                ).await()
            }
        } catch (_: ApiException) {
            onError(API_EXCEPTION)
        }
    }

    override suspend fun publish(config: LauncherConfig, icons: Map<String, ByteArray>) {
        val payload = PayloadCodec.encodeConfig(config)
        require(PayloadCodec.fitsDataItem(payload))
        val request = PutDataMapRequest.create(SyncPaths.CONFIG_DESIRED)
        request.dataMap.putInt(SyncKeys.SCHEMA, ProtocolVersion.CURRENT)
        request.dataMap.putByteArray(SyncKeys.PAYLOAD, payload)
        for ((id, bytes) in icons) {
            request.dataMap.putAsset(SyncKeys.iconAssetKey(id), Asset.createFromBytes(bytes))
        }
        data.putDataItem(request.asPutDataRequest().setUrgent()).await()
    }

    override fun observe(onChanged: () -> Unit) {
        if (listener != null) return
        val callback = CapabilityClient.OnCapabilityChangedListener { _: CapabilityInfo -> onChanged() }
        listener = callback
        capability.addListener(callback, Capabilities.WATCH_APP).addOnFailureListener { onChanged() }
    }

    override fun stopObserving() {
        listener?.let { capability.removeListener(it, Capabilities.WATCH_APP) }
        listener = null
    }

    override suspend fun readExisting() {
        val buffer = data.dataItems.await()
        try {
            for (item in buffer) receiver.receive(item)
        } finally {
            buffer.release()
        }
    }

    private companion object {
        const val API_EXCEPTION = "ApiException"
    }
}
