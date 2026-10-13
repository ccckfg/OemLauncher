package com.n2.oemlauncher.shared.layout

import com.n2.oemlauncher.shared.model.IconSize

/**
 * Honeycomb tuning shared by the watch renderer and the phone preview. Changing a value here
 * changes both, which is the point: the preview must match the watch.
 */
object HoneycombDefaults {
    const val ICON_DIAMETER_SMALL_DP = 40f
    const val ICON_DIAMETER_MEDIUM_DP = 52f
    const val ICON_DIAMETER_LARGE_DP = 64f

    /** Center-to-center distance divided by the icon diameter. */
    const val SPACING_RATIO = 1.18f

    /** Fraction of the viewport radius in which icons keep full size. */
    const val FLAT_ZONE = 0.3f

    /** Smallest icon scale at the rim when the fisheye strength is 1. */
    const val MIN_SCALE_AT_FULL_STRENGTH = 0.3f

    /** Normalized distance (icon edge) where icons start fading, and where they vanish. */
    const val FADE_START = 0.9f
    const val FADE_END = 1.04f

    /** Icons fainter than this are not tappable. */
    const val MIN_TAPPABLE_ALPHA = 0.4f

    /** Minimum touch target; nearest-center hit testing keeps enlarged targets disjoint. */
    const val MIN_TOUCH_DIAMETER_DP = 48f

    /** Overscroll distance, as a fraction of the viewport radius, where rubber-banding saturates. */
    const val RUBBER_BAND_FRACTION = 0.35f

    fun iconDiameterDp(size: IconSize): Float = when (size) {
        IconSize.SMALL -> ICON_DIAMETER_SMALL_DP
        IconSize.MEDIUM -> ICON_DIAMETER_MEDIUM_DP
        IconSize.LARGE -> ICON_DIAMETER_LARGE_DP
    }

    fun pitchFor(iconDiameterPx: Float): Float = iconDiameterPx * SPACING_RATIO
}
