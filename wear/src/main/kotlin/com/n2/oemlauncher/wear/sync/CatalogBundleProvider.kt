package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.wear.data.IconStore
import com.n2.oemlauncher.wear.data.UsageStats

/**
 * Read side of the catalog the sync layer publishes. Keeping this interface free of any
 * "rebuild the icons" API is what prevents the publish path from re-triggering a full refresh
 * (and therefore an endless refresh -> publish -> refresh loop).
 */
interface CatalogSource {
    /** Last catalog the watch built, or null when it has never built one. */
    fun snapshot(): WatchCatalog?

    /** Builds and stores a fresh catalog; only used when no snapshot exists yet. */
    suspend fun rebuild(config: LauncherConfig, usage: UsageStats): WatchCatalog
}

/**
 * Assembles the catalog DataItem from the snapshot on disk plus the default icon bytes already
 * stored, merging the current usage counters. The heavy LauncherApps/icon rebuild only runs
 * when there is no snapshot at all (for example a request-catalog arriving before the launcher
 * UI was ever opened).
 */
class CatalogBundleProvider(
    private val source: CatalogSource,
    private val iconStore: IconStore,
    private val usage: suspend () -> UsageStats,
) : CatalogProvider {

    override suspend fun load(config: LauncherConfig): CatalogBundle {
        val stats = usage()
        val current = source.snapshot() ?: source.rebuild(config, stats)
        val entries = current.entries.map { entry ->
            val record = stats[entry.key]
            if (record == null) entry else entry.copy(
                launchCount = record.launchCount,
                lastLaunchedMillis = record.lastLaunchedMillis,
            )
        }
        val catalog = current.copy(entries = entries, generatedAtMillis = System.currentTimeMillis())
        val icons = entries
            .mapNotNull { entry -> entry.iconId?.let { id -> iconStore.get(id)?.let { id to it } } }
            .toMap()
        return CatalogBundle(catalog, icons)
    }
}
