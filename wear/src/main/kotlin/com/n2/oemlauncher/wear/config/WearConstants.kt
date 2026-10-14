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
}
