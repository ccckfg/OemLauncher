package com.n2.oemlauncher.wear.ui

import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombHitTest

/** Pure touch-to-cell mapping used by the canvas and JVM tests. */
fun honeycombHitIndex(
    frame: HoneycombFrame,
    touchX: Float,
    touchY: Float,
    iconRadiusPx: Float,
    minTouchRadiusPx: Float,
    minAlpha: Float = com.n2.oemlauncher.shared.layout.HoneycombDefaults.MIN_TAPPABLE_ALPHA,
): Int = HoneycombHitTest.hit(frame, touchX, touchY, iconRadiusPx, minTouchRadiusPx, minAlpha)
