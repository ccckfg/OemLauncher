package com.n2.oemlauncher.wear.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import com.n2.oemlauncher.wear.config.WearUiTokens
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Transforms that make straight rows sit on a round screen. A row's position is its vertical
 * center on screen as a fraction: 0 at the top edge, 0.5 in the middle, 1 at the bottom edge.
 */
object RoundEdge {
    /** 1 in the flat middle band, easing (smoothstep) to [min] at the top and bottom edges. */
    fun falloff(position: Float, min: Float, flat: Float = WearUiTokens.EDGE_FLAT_FRACTION): Float {
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

/**
 * Shrinks and fades a row towards the edges and, with [arcPx] > 0, shifts it right so a
 * left-aligned row follows the circle. Reads scroll progress in the draw phase only.
 */
fun Modifier.roundEdge(scope: TransformingLazyColumnItemScope, arcPx: Float = 0f): Modifier =
    with(scope) {
        this@roundEdge.graphicsLayer {
            val progress = scrollProgress
            if (progress.isUnspecified) return@graphicsLayer
            val position = (progress.topOffsetFraction + progress.bottomOffsetFraction) / 2f
            val scale = RoundEdge.falloff(position, WearUiTokens.EDGE_MIN_SCALE)
            scaleX = scale
            scaleY = scale
            alpha = RoundEdge.falloff(position, WearUiTokens.EDGE_MIN_ALPHA)
            if (arcPx > 0f) {
                transformOrigin = TransformOrigin(0f, 0.5f)
                translationX = RoundEdge.arcInset(position) * arcPx
            }
        }
    }
