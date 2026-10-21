package com.n2.oemlauncher.mobile.config

object ImportLimits {
    const val MAX_ENTRIES = 500
    const val MAX_FILE_BYTES = 2 * 1024 * 1024
    const val MAX_TOTAL_BYTES = 50 * 1024 * 1024
    const val BUFFER_BYTES = 8192
    const val MAX_SOURCE_PIXELS = 40_000_000L
    const val PHOTO_MAX_BYTES = 20 * 1024 * 1024
    const val DECODE_EDGE_PX = 1024
    const val MAX_ZOOM = 5f
    const val XML_COMPONENT_PREFIX = "ComponentInfo{"
    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
    val ZIP_MIME_TYPES = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
}

object SyncLimits {
    const val ASSET_FETCH_CONCURRENCY = 4
}

object StorageConstants {
    const val DATASTORE_FILE = "companion.preferences_pb"
    const val SNAPSHOT_KEY = "snapshot"
    const val ICON_DIRECTORY = "icons"
    const val STAGING_DIRECTORY = "icons-staging"
    const val TEMP_ICON_SUFFIX = ".tmp"
    const val ICON_SUFFIX = ".png"
    const val BITMAP_CACHE_BYTES = 12 * 1024 * 1024
    const val PNG_QUALITY = 100
    val PNG_QUANTIZATION_MASKS = intArrayOf(0xF8, 0xF0, 0xE0)
    const val TAG = "N2Companion"
}

object UiDimensions {
    const val PAGE_PADDING = 20
    const val GAP = 12
    const val SMALL_GAP = 8
    /** Detail lines get room to wrap; titles stay single line. */
    const val DETAIL_MAX_LINES = 3
    const val TOUCH_TARGET = 48
    const val APP_ICON = 44
    const val WIDE_BREAKPOINT = 720
    const val PREVIEW_PANE = 320
    const val DRAG_THRESHOLD = 56
    const val ANIMATION_MS = 180
    const val CROP_SCRIM_ALPHA = 0.65f
}

object DemoConstants {
    const val SIZE = 40
    const val WIDTH_PX = 454
    const val DENSITY_DPI = 320
    const val SDK = 36
    /** Shown as the demo watch's app version; bump it to redraw stored demo icons. */
    const val VERSION = "1.1.0"
    const val PACKAGE_PREFIX = "com.n2.demo."
    const val CLASS_SUFFIX = ".MainActivity"
    const val ICON_INSET_RATIO = 0.18f

    /**
     * Muted earth tones for the stand-in icons: saturated hues fought the mocha page. Cream
     * glyphs keep at least 6:1 on every tone.
     */
    val TONES = longArrayOf(0xFF3E4B5E, 0xFF4C5A3A, 0xFF75582C, 0xFF7A4434, 0xFF5C3D52, 0xFF2F5655, 0xFF5A4636)
    const val INK = 0xFFFFF7EE
    const val GLYPH_RATIO = 0.5f
    const val SHEEN_COLOR = 0x24FFF3E6
    const val SHEEN_FRACTION = 0.6f
}

object SearchConstants {
    /** Polyphonic labels multiply spellings per character; cap them so one label stays cheap. */
    const val MAX_SPELLINGS = 16

    /** Keeps the index across quick screen switches instead of rebuilding it each time. */
    const val INDEX_KEEP_ALIVE_MS = 5_000L
}
