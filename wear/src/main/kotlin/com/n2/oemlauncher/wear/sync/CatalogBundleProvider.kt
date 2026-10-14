package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.wear.catalog.AppCatalogRepository
import com.n2.oemlauncher.wear.data.ConfigRepository
import com.n2.oemlauncher.wear.data.IconStore
import kotlinx.coroutines.flow.first

/**
 * Builds the catalog payload from the live platform state: a fresh LauncherApps query plus the
 * default icon bytes already on disk for the entries it references.
 */
class CatalogBundleProvider(
    private val configRepository: ConfigRepository,
    private val catalogRepository: AppCatalogRepository,
    private val iconStore: IconStore,
) : CatalogProvider {

    override suspend fun load(config: LauncherConfig): CatalogBundle {
        val usage = configRepository.usage.first()
        val catalog = catalogRepository.refresh(config, usage)
        val icons = catalog.entries
            .mapNotNull { entry -> entry.iconId?.let { id -> iconStore.get(id)?.let { id to it } } }
            .toMap()
        return CatalogBundle(catalog, icons)
    }
}
