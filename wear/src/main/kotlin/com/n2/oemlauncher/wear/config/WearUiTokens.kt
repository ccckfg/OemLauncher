package com.n2.oemlauncher.wear.config

/** Dimensions, motion and visual thresholds of the watch UI. Colors live in ui/theme. */
object WearUiTokens {
    /** Gap between the screen edge and the circle the honeycomb fits its icons into. */
    const val HONEYCOMB_EDGE_INSET_DP = 6f

    /** Fraction of the screen radius where the edge vignette starts to darken content. */
    const val VIGNETTE_START_FRACTION = 0.74f
    const val VIGNETTE_ALPHA = 0.9f

    /** Size of the gear glyph relative to its honeycomb cell, and its ring stroke. */
    const val GEAR_GLYPH_FRACTION = 0.52f
    const val GEAR_RING_DP = 1.5f
    const val COOKIE_RADIUS_DP = 52f
    const val COOKIE_BUTTON_DP = 52f
    const val COOKIE_GEAR_FRACTION = 0.5f
    const val COOKIE_PRESS_SCALE = 0.94f
    const val APP_RIM_DP = 1f
    const val FAVORITE_RIM_DP = 1.5f
    const val ICON_CORNER_FRACTION = 0.22f
    const val COOKIE_SHADOW_OFFSET_FRACTION = 0.08f
    const val BACKDROP_CORE_Y = 0.42f
    const val BACKDROP_CORE_RADIUS = 1.1f
    const val BACKDROP_MIST_X = 0.7f
    const val BACKDROP_MIST_Y = 0.72f
    const val BACKDROP_MIST_RADIUS = 0.85f
    const val GLASS_ROW_HEIGHT_DP = 52f
    const val GLASS_RIM_FADE = 0.55f
    const val MOTION_SHORT_MS = 150
    const val MOTION_STANDARD_MS = 260
    const val MOTION_EMPHASIS_MS = 380

    /** Curved name of the centered honeycomb cell along the bottom bezel. */
    const val FOCUS_LABEL_SP = 13f
    const val FOCUS_LABEL_SWEEP_DEGREES = 80f
    const val FOCUS_LABEL_FADE_MS = 160

    /** Grid and list rows shrink and fade towards these values at the top and bottom edges. */
    const val EDGE_MIN_SCALE = 0.7f
    const val EDGE_MIN_ALPHA = 0.4f

    /** Middle share of the screen height where rows keep their full size. */
    const val EDGE_FLAT_FRACTION = 0.34f

    /** How far list rows follow the circle, as a share of the screen radius. */
    const val LIST_ARC_FACTOR = 0.55f

    const val GRID_HORIZONTAL_PADDING_DP = 24f
    const val GRID_SPACING_DP = 6f
    const val GRID_CELL_PADDING_DP = 2f
    const val GRID_LABEL_SP = 10f
    const val GRID_VERTICAL_PADDING_DP = 40f

    const val LIST_ICON_DP = 38f
    const val LIST_ROW_MIN_HEIGHT_DP = 52f
    const val LIST_LABEL_SP = 14f
    const val LIST_START_PADDING_DP = 30f
    const val LIST_END_PADDING_DP = 18f
    const val LIST_VERTICAL_PADDING_DP = 44f
    const val LIST_ROW_SPACING_DP = 2f
    const val LIST_LABEL_GAP_DP = 12f

    /** Crown scrolling: pixels per rotary tick in scrolling screens. */
    const val ROTARY_SCROLL_PX = 56f

    const val SWITCHER_BUTTON_DP = 52f
    const val SWITCHER_GAP_DP = 4f
    const val SWITCHER_PADDING_DP = 6f
    const val SWITCHER_BLUR_DP = 14f
    const val SCRIM_ALPHA = 0.55f

    /** Fisheye strength is edited in tenths. */
    const val FISHEYE_STEPS = 10

    /** Choice pages open centered on the selected option once it is past this many options. */
    const val CHOICE_VISIBLE_OPTIONS = 2

    const val MENU_ICON_DP = 52f
    const val MESSAGE_BOTTOM_PADDING_DP = 42f
    const val CARD_PADDING_DP = 12f
}
