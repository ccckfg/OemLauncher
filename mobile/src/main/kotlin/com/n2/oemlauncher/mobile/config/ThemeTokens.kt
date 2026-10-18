package com.n2.oemlauncher.mobile.config

/**
 * Thresholds of the theme. Colors are never defined here: both palettes come from
 * `shared/.../design/DesignPalette.kt` so the phone and the watch cannot drift apart.
 */
object ThemeTokens {
    /** Above this luminance the surface is light and the system bars need dark icons. */
    const val LIGHT_SURFACE_THRESHOLD = 0.5f

    /** The phone only blurs overlay windows; cards sit on a static backdrop. */
    const val OVERLAY_BLUR_DP = 24f
}
