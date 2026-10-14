package com.n2.oemlauncher.wear.sync

import android.content.Context
import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.SyncPaths
import com.n2.oemlauncher.wear.data.ConfigRepository
import com.n2.oemlauncher.wear.data.IconFileStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WearSyncRepository(
    private val context: Context,
    private val configRepository: ConfigRepository,
    private val iconStore: IconFileStore,
    private val client: DataLayerClient,
) {
    private val mutex = Mutex()
    private val publisher = CatalogPublisher(client)
    private var state = WatchConfigState(LauncherConfig())
    private val processor = DesiredProcessor(configRepository, iconStore)

    suspend fun handleDesired(schema: Int, payload: ByteArray, assets: Map<String, ByteArray>) = mutex.withLock {
        val result = processor.process(schema, payload, assets, state, System.currentTimeMillis())
        if (result.applied || result.state.lastResult != ApplyResult.NONE) {
            state = result.state
            publishStateLocked(state)
        }
        result
    }

    suspend fun publishState(config: LauncherConfig) = mutex.withLock {
        state = state.copy(current = config, updatedAtMillis = System.currentTimeMillis())
        publishStateLocked(state)
    }

    suspend fun publishCatalog(catalog: com.n2.oemlauncher.shared.model.WatchCatalog, icons: Map<String, ByteArray>, force: Boolean = false) =
        mutex.withLock { publisher.publish(catalog, icons, force) }

    private suspend fun publishStateLocked(next: WatchConfigState) {
        client.put(SyncPaths.CONFIG_STATE, ProtocolVersion.CURRENT, PayloadCodec.encodeState(next), emptyMap(), urgent = false)
    }
}
