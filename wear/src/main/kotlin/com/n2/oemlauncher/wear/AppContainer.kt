package com.n2.oemlauncher.wear

import android.content.Context
import com.n2.oemlauncher.wear.catalog.AppCatalogRepository
import com.n2.oemlauncher.wear.catalog.LauncherAppsObserver
import com.n2.oemlauncher.wear.data.CatalogSnapshotStore
import com.n2.oemlauncher.wear.data.ConfigRepository
import com.n2.oemlauncher.wear.data.IconFileStore
import com.n2.oemlauncher.wear.sync.DataLayerClient
import com.n2.oemlauncher.wear.sync.GoogleDataLayerClient
import com.n2.oemlauncher.wear.sync.WearSyncRepository

class AppContainer(context: Context) {
    val configRepository = ConfigRepository(context)
    val iconStore = IconFileStore(context.filesDir)
    val snapshotStore = CatalogSnapshotStore(context.filesDir)
    val catalogRepository = AppCatalogRepository(context, configRepository, snapshotStore, iconStore)
    val dataLayerClient: DataLayerClient = GoogleDataLayerClient(context)
    val syncRepository = WearSyncRepository(context, configRepository, iconStore, dataLayerClient)
    val catalogObserver = LauncherAppsObserver(context) { }
}
