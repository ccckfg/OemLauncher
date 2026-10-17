package com.n2.oemlauncher.mobile.config

/**
 * Geometry of the watch preview. The phone draws the watch with the exact numbers the wear
 * module uses, so the wear module must not be imported: `RoundEdge` and the edge token values
 * below are a port of `wear/.../ui/RoundEdge.kt` and `wear/.../config/WearUiTokens.kt`.
 * Keep them in sync with that file.
 */
object PreviewTokens {
    /** Watch labels, in sp. The watch never goes below 12sp (design section 5). */
    const val GRID_LABEL_SP = 12f
    const val LIST_LABEL_SP = 14f
    const val FOCUS_LABEL_SP = 13f

    /** The focus name follows the bottom bezel: an arc of this sweep at this radius fraction. */
    const val FOCUS_LABEL_SWEEP_DEGREES = 85f
    const val FOCUS_LABEL_RADIUS_FRACTION = 0.84f

    /** Centered settings gear, in dp. The same size the watch uses for its grid and list row. */
    const val GEAR_BUTTON_DP = 52f
    const val GEAR_GLYPH_FRACTION = 0.5f

    /** Rings around app icons: a hairline rim, and champagne for favorites. */
    const val ICON_RIM_DP = 1f
    const val FAVORITE_RING_DP = 1.5f
    const val FAVORITE_RING_GAP_DP = 1.5f

    /** Grid and list insets, in watch dp. */
    const val GRID_TOP_DP = 40f
    const val GRID_LABEL_GAP_DP = 20f
    const val LIST_ROW_DP = 64f
    const val LIST_ARC_FACTOR = 0.55f
    const val ICON_CORNER_RATIO = 0.24f

    /** Rows shrink and fade towards the edges; WearUiTokens.EDGE_*. */
    const val EDGE_MIN_SCALE = 0.7f
    const val EDGE_MIN_ALPHA = 0.4f
    const val EDGE_FLAT_FRACTION = 0.34f

    /** Refined backdrop: warm glow above the center, cool glow bottom right, edge vignette. */
    const val WARM_GLOW_Y = 0.36f
    const val WARM_GLOW_STOP = 0.62f
    const val WARM_GLOW_ALPHA = 0.95f
    const val COOL_GLOW_X = 0.8f
    const val COOL_GLOW_Y = 0.82f
    const val COOL_GLOW_STOP = 0.46f
    const val COOL_GLOW_ALPHA = 0.75f
    const val VIGNETTE_START_FRACTION = 0.6f
    const val VIGNETTE_ALPHA = 0.55f

    /** Cookie outline rim, in watch dp, and the gear's soft shadow. */
    const val GEAR_RIM_DP = 0.75f
    const val GEAR_SHADOW_DP = 8f
    const val GEAR_SHADOW_ALPHA = 0.5f

    /** The gear glyph is rasterised once at this edge length and then scaled down. */
    const val GEAR_BITMAP_PX = 96
}
