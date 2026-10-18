package com.n2.oemlauncher.wear.catalog

import com.n2.oemlauncher.shared.model.IconStyle

enum class IconRenderMode { OVERRIDE, THEMED, ORIGINAL }

/** Pure policy used before touching AdaptiveIconDrawable, so the fallback is easy to test. */
fun iconRenderMode(
    style: IconStyle,
    hasMonochromeLayer: Boolean,
    overrideAvailable: Boolean = false,
): IconRenderMode = when {
    overrideAvailable -> IconRenderMode.OVERRIDE
    style == IconStyle.THEMED && hasMonochromeLayer -> IconRenderMode.THEMED
    else -> IconRenderMode.ORIGINAL
}
