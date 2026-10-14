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
    const val TOUCH_TARGET = 48
    const val APP_ICON = 44
    const val PREVIEW = 280
    const val THUMBNAIL = 216
    const val WIDE_BREAKPOINT = 720
    const val PREVIEW_PANE = 320
    const val PREVIEW_FOOTER = 140
    const val ROW_HEIGHT = 112
    const val DRAG_THRESHOLD = 56
    const val CORNER = 20
    const val PREVIEW_BORDER = 6
    const val PREVIEW_LABEL_DP = 12f
    const val LIST_ROW_DP = 64f
    const val GRID_LABEL_DP = 20f
    const val GRID_TOP_DP = 24f
    const val ICON_CORNER_RATIO = 0.24f
    const val ANIMATION_MS = 180
    const val LABEL_INSET_RATIO = 0.10f
    const val LABEL_BAND_LINES = 2.6f
    const val CROP_SCRIM_ALPHA = 0.65f
}

object DemoConstants {
    const val SIZE = 40
    const val WIDTH_PX = 454
    const val DENSITY_DPI = 320
    const val SDK = 36
    const val VERSION = "1.0.0"
    const val PACKAGE_PREFIX = "com.n2.demo."
    const val CLASS_SUFFIX = ".MainActivity"
    const val ICON_TEXT_RATIO = 0.42f
    const val ICON_INSET_RATIO = 0.18f
    const val PALETTE_SEED = 137
    const val PALETTE_STEPS = 360
    const val PALETTE_SATURATION = 0.58f
    const val PALETTE_VALUE = 0.83f
}
