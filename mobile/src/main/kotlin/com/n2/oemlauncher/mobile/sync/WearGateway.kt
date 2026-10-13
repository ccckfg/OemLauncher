package com.n2.oemlauncher.mobile.sync

import com.n2.oemlauncher.mobile.data.WatchConnection
import com.n2.oemlauncher.shared.model.LauncherConfig

interface WearGateway {
    suspend fun discover(): WatchConnection
    suspend fun requestCatalog()
    suspend fun publish(config: LauncherConfig, icons: Map<String, ByteArray>)
    fun observe(onChanged: () -> Unit)
    fun stopObserving()
    suspend fun readExisting()
}
