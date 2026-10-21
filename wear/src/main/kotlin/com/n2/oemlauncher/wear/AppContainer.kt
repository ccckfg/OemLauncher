package com.n2.oemlauncher.wear

import android.content.Context
import com.n2.oemlauncher.wear.catalog.AppCatalogRepository
import com.n2.oemlauncher.wear.catalog.AppListPermissionController
import com.n2.oemlauncher.wear.catalog.LauncherAppsObserver
import com.n2.oemlauncher.wear.data.CatalogSnapshotStore
import com.n2.oemlauncher.wear.data.ConfigRepository
import com.n2.oemlauncher.wear.data.CustomIconRepository
import com.n2.oemlauncher.wear.data.FileCustomIconRepository
import com.n2.oemlauncher.wear.data.IconFileStore
import com.n2.oemlauncher.wear.sync.CatalogBundleProvider
import com.n2.oemlauncher.wear.sync.DataLayerClient
import com.n2.oemlauncher.wear.sync.GoogleDataLayerClient
import com.n2.oemlauncher.wear.sync.WearSyncRepository
import com.n2.oemlauncher.wear.tile.FavoritesTileUpdater
import com.n2.oemlauncher.wear.home.ForegroundStateHolder
import com.n2.oemlauncher.wear.home.HomeSettingsRepository
import com.n2.oemlauncher.wear.home.SystemHomeResolver
import com.n2.oemlauncher.wear.config.WearDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val foregroundState = ForegroundStateHolder(context.packageName)
    val systemHomeResolver = SystemHomeResolver(context)
    val homeSettingsRepository = HomeSettingsRepository(context)
    val appListPermission = AppListPermissionController(context)
    val configRepository = ConfigRepository(context)
    val iconStore = IconFileStore(context.filesDir)
    val snapshotStore = CatalogSnapshotStore(context.filesDir)
    val customIconRepository: CustomIconRepository = FileCustomIconRepository(iconStore)
    val catalogRepository = AppCatalogRepository(
        context,
        configRepository,
        snapshotStore,
        iconStore,
        customIconRepository,
        appListPermission,
    )
    val dataLayerClient: DataLayerClient = GoogleDataLayerClient(context)
    val syncRepository = WearSyncRepository(
        store = configRepository,
        iconStore = iconStore,
        client = dataLayerClient,
        catalogProvider = CatalogBundleProvider(
            source = catalogRepository,
            iconStore = iconStore,
            usage = { configRepository.usage.first() },
        ),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        onStateChanged = { FavoritesTileUpdater.request(context) },
    )
    val catalogObserver = LauncherAppsObserver(context) { }
    private val homeResolverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        refreshSystemHomePackage()
    }

    fun refreshSystemHomePackage() {
        homeResolverScope.launch {
            val packageName = systemHomeResolver.packageName()
            foregroundState.systemHomePackage = packageName
            WearDebugLog.d { "container init systemHomePackage=${packageName ?: "null"}" }
        }
    }
}
