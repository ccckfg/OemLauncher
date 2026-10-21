package com.n2.oemlauncher.wear.catalog

import com.n2.oemlauncher.shared.model.IconStyle

enum class IconRenderMode { OVERRIDE, PACK, THEMED, ORIGINAL }

/**
 * Pure policy used before touching AdaptiveIconDrawable, so the fallback is easy to test. The
 * pack comes before the app's own monochrome layer: its glyphs share one stroke and one set of
 * tones, where monochrome layers range from hairlines to solid shapes.
 */
fun iconRenderMode(
    style: IconStyle,
    hasMonochromeLayer: Boolean,
    overrideAvailable: Boolean = false,
    inPack: Boolean = false,
): IconRenderMode = when {
    overrideAvailable -> IconRenderMode.OVERRIDE
    style == IconStyle.THEMED && inPack -> IconRenderMode.PACK
    style == IconStyle.THEMED && hasMonochromeLayer -> IconRenderMode.THEMED
    else -> IconRenderMode.ORIGINAL
}
