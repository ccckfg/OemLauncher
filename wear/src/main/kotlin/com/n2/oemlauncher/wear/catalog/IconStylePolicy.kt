package com.n2.oemlauncher.wear.catalog

import com.n2.oemlauncher.shared.model.IconStyle

enum class IconRenderMode { ORIGINAL, THEMED }

/** Pure policy used before touching AdaptiveIconDrawable, so the fallback is easy to test. */
fun iconRenderMode(style: IconStyle, hasMonochromeLayer: Boolean): IconRenderMode =
    if (style == IconStyle.THEMED && hasMonochromeLayer) IconRenderMode.THEMED
    else IconRenderMode.ORIGINAL
