package com.n2.oemlauncher.shared.layout

/** Unit conversion for drawing the watch screen on another device. */
object DisplayMath {
    private const val DENSITY_BASELINE_DPI = 160f

    fun dpToPx(dp: Float, densityDpi: Int): Float = dp * densityDpi / DENSITY_BASELINE_DPI

    fun pxToDp(px: Float, densityDpi: Int): Float =
        if (densityDpi <= 0) px else px * DENSITY_BASELINE_DPI / densityDpi
}
