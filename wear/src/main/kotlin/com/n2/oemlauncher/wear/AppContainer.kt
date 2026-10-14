package com.n2.oemlauncher.wear

import android.content.Context
import com.n2.oemlauncher.wear.catalog.AppCatalogRepository
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    val configRepository = ConfigRepository(context)
    val iconStore = IconFileStore(context.filesDir)
    val snapshotStore = CatalogSnapshotStore(context.filesDir)
    val catalogRepository = AppCatalogRepository(context, configRepository, snapshotStore, iconStore)
    val dataLayerClient: DataLayerClient = GoogleDataLayerClient(context)
    val syncRepository = WearSyncRepository(
        store = configRepository,
        iconStore = iconStore,
        client = dataLayerClient,
        catalogProvider = CatalogBundleProvider(configRepository, catalogRepository, iconStore),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        onStateChanged = { FavoritesTileUpdater.request(context) },
    )
    val catalogObserver = LauncherAppsObserver(context) { }
    val customIconRepository: CustomIconRepository = FileCustomIconRepository(iconStore)
}
