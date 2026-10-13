package com.n2.oemlauncher.mobile

import android.content.Context
import com.n2.oemlauncher.mobile.data.CompanionRepository
import com.n2.oemlauncher.mobile.data.DemoCatalogFactory
import com.n2.oemlauncher.mobile.data.IconStore
import com.n2.oemlauncher.mobile.data.PreferencesSnapshotStore
import com.n2.oemlauncher.mobile.iconpack.IconImportRepository
import com.n2.oemlauncher.mobile.iconpack.IconProcessor
import com.n2.oemlauncher.mobile.sync.GoogleWearGateway
import com.n2.oemlauncher.mobile.sync.WearDataReceiver
import com.n2.oemlauncher.mobile.sync.WearGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val icons = IconStore(context.filesDir)
    val repository = CompanionRepository(PreferencesSnapshotStore(context, appScope), icons, appScope)
    val receiver = WearDataReceiver(context, repository)
    val gateway: WearGateway = GoogleWearGateway(context, receiver)
    private val processor = IconProcessor()
    val importer = IconImportRepository(context.contentResolver, repository, processor)
    val demo = DemoCatalogFactory(context, icons, processor)
}
