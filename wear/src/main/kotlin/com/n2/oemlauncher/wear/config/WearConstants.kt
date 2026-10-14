package com.n2.oemlauncher.wear.config

object WearConstants {
    const val ICON_CACHE_BYTES = 8L * 1024L * 1024L
    const val ICON_CACHE_DIR = "icons"
    const val ICON_TARGET_PX = 128
    const val CATALOG_THROTTLE_MS = 10_000L
    const val USAGE_PUBLISH_EVERY = 5
    const val DEMO_ENTRY_COUNT = 128
    const val SNAPSHOT_FILE = "catalog.json"
    const val CONFIG_KEY = "launcher_config"
    const val USAGE_KEY = "usage_stats"
    const val STATE_KEY = "sync_state"
    const val LAST_SYNC_KEY = "last_sync_millis"

    /** True while the persisted sync state has not been acknowledged by the phone. */
    const val STATE_DIRTY_KEY = "sync_state_dirty"

    /** Upper bound of icon assets downloaded from the Data Layer at the same time. */
    const val ICON_FETCH_CONCURRENCY = 4

    /** Favorites the tile renders; the launcher itself keeps LauncherDefaults.MAX_FAVORITES. */
    const val TILE_FAVORITES_MAX = 4

    /** Version of the (empty) tile resource bundle; bump when tile resources change. */
    const val TILE_RESOURCES_VERSION = "1"

    /** The system re-requests tile content after this interval, even without an update call. */
    const val TILE_FRESHNESS_MILLIS = 60 * 60 * 1000L

    /** Icons are square PNGs/WebPs; intrinsic-size hint for inline tile images. */
    const val TILE_ICON_PX = 128

    /** Rendered edge of a favorite icon in the tile. */
    const val TILE_ICON_DP = 40f

    /** Minimum touch target for tile rows and buttons, per accessibility rules. */
    const val TILE_TOUCH_TARGET_DP = 48f

    /** Font sizes of the tile header and of favorite labels. */
    const val TILE_HEADER_SP = 16f
    const val TILE_LABEL_SP = 14f
}
