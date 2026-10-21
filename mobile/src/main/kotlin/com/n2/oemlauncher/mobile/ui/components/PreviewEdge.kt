package com.n2.oemlauncher.mobile.ui.components

import com.n2.oemlauncher.mobile.config.PreviewTokens
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Straight-row transforms of the watch UI, ported from `wear/.../ui/RoundEdge.kt` so the preview
 * grid and list shrink and fade exactly like the watch. Keep the math in sync with that file.
 * A row's position is its vertical center as a fraction of the screen height.
 */
object PreviewEdge {
    /** 1 in the flat middle band, easing (smoothstep) to [min] at the top and bottom edges. */
    fun falloff(position: Float, min: Float, flat: Float = PreviewTokens.EDGE_FLAT_FRACTION): Float {
        val distance = (abs(position - 0.5f) * 2f).coerceIn(0f, 1f)
        val t = ((distance - flat) / (1f - flat)).coerceIn(0f, 1f)
        val eased = t * t * (3f - 2f * t)
        return 1f - (1f - min) * eased
    }

    /**
     * How far the circle's left edge sits from the screen's left edge at this height, in units
     * of the radius: 0 on the center line, 1 at the very top or bottom.
     */
    fun arcInset(position: Float): Float {
        val dy = ((position - 0.5f) * 2f).coerceIn(-1f, 1f)
        return 1f - sqrt(1f - dy * dy)
    }
}
