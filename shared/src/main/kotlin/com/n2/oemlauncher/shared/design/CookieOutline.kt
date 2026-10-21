package com.n2.oemlauncher.shared.design

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Outline of the scalloped "cookie" disc behind the center settings gear, the one signature
 * shape of the design. Callers build their path once from these points and reuse it.
 */
object CookieOutline {
    const val SCALLOPS = 9

    /** Depth of each scallop as a fraction of the radius. */
    const val DEPTH = 0.09f

    private const val STEPS_PER_SCALLOP = 12

    /**
     * Points of the closed outline as x0, y0, x1, y1, ... relative to the center. The radius
     * swings between `radius * (1 - depth)` and `radius`, with a bump pointing straight up.
     */
    fun points(radius: Float, scallops: Int = SCALLOPS, depth: Float = DEPTH): FloatArray {
        val count = scallops * STEPS_PER_SCALLOP
        val out = FloatArray(count * 2)
        val half = depth / 2f
        for (i in 0 until count) {
            val angle = 2.0 * PI * i / count
            val r = radius * (1f - half + half * cos(scallops * angle).toFloat())
            // Angle 0 points up so a bump crowns the shape.
            out[i * 2] = (r * sin(angle)).toFloat()
            out[i * 2 + 1] = (-r * cos(angle)).toFloat()
        }
        return out
    }
}
