package com.n2.oemlauncher.shared.layout

import kotlin.math.max

object HoneycombHitTest {

    /**
     * Index of the tapped cell, or -1. Among visible cells whose touch radius covers the point,
     * the one with the nearest center wins, so enlarged touch targets never overlap.
     * Coordinates are relative to the viewport center, like [HoneycombFrame].
     */
    fun hit(
        frame: HoneycombFrame,
        touchX: Float,
        touchY: Float,
        iconRadiusPx: Float,
        minTouchRadiusPx: Float,
        minAlpha: Float = HoneycombDefaults.MIN_TAPPABLE_ALPHA,
    ): Int {
        var best = -1
        var bestDistance = Float.MAX_VALUE
        for (i in 0 until frame.size) {
            if (frame.alpha[i] < minAlpha) continue
            val dx = frame.x[i] - touchX
            val dy = frame.y[i] - touchY
            val distance = dx * dx + dy * dy
            if (distance >= bestDistance) continue
            val reach = max(iconRadiusPx * frame.scale[i], minTouchRadiusPx)
            if (distance <= reach * reach) {
                best = i
                bestDistance = distance
            }
        }
        return best
    }
}
