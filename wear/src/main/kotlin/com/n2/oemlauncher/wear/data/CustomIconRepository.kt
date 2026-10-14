package com.n2.oemlauncher.wear.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read side of the icons the launcher renders: the default icons published with the catalog
 * plus the custom icons the phone synced. The sync pipeline is the only writer.
 *
 * Callers resolve an app to an icon id with `LauncherConfig.iconOverrides` (falling back to
 * `CatalogEntry.iconId`) and ask here for the bytes; decoding and caching happen on the UI
 * side, see `AppCatalogRepository.loadIcon` for the launcher's own path.
 */
interface CustomIconRepository {
    /** Raw PNG/WebP bytes for [iconId], or null when that icon is not on disk. */
    suspend fun bytes(iconId: String): ByteArray?
}

class FileCustomIconRepository(private val store: IconFileStore) : CustomIconRepository {
    override suspend fun bytes(iconId: String): ByteArray? = withContext(Dispatchers.IO) { store.get(iconId) }
}
