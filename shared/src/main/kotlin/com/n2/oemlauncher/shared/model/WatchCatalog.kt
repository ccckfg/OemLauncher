package com.n2.oemlauncher.shared.model

import kotlinx.serialization.Serializable

/**
 * One launchable app as reported by the watch.
 *
 * [iconId] identifies the default icon shipped as an Asset next to the catalog. Usage counters
 * only cover launches made through this launcher; they let the phone preview match the watch
 * when the sort mode depends on usage.
 */
@Serializable
data class CatalogEntry(
    val key: String,
    val label: String,
    val iconId: String? = null,
    val isSystem: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchedMillis: Long = 0,
)

/** Physical screen of the watch, so the phone can draw a true-to-size preview. */
@Serializable
data class WatchDisplay(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val isRound: Boolean,
)

/** Everything the phone needs to edit the launcher: installed apps plus device facts. */
@Serializable
data class WatchCatalog(
    val generatedAtMillis: Long,
    val deviceName: String,
    val sdkInt: Int,
    val appVersionName: String,
    val display: WatchDisplay,
    val entries: List<CatalogEntry>,
)
