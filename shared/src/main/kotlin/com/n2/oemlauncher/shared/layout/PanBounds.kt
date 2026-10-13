package com.n2.oemlauncher.shared.layout

import kotlin.math.hypot

/**
 * Circular pan limit: any cell can be brought to the center, but the content cannot be dragged
 * further away. Results go into a caller-owned FloatArray of size 2 to stay allocation-free.
 */
class PanBounds(val limitPx: Float, private val rubberBandPx: Float) {

    fun isOutside(x: Float, y: Float): Boolean = x * x + y * y > limitPx * limitPx

    fun clamp(x: Float, y: Float, out: FloatArray) {
        val distance = hypot(x, y)
        val ratio = if (distance > limitPx && distance > 0f) limitPx / distance else 1f
        out[0] = x * ratio
        out[1] = y * ratio
    }

    /** Maps an unconstrained drag position to a resisted one: free inside, saturating outside. */
    fun rubberBand(x: Float, y: Float, out: FloatArray) {
        val distance = hypot(x, y)
        if (distance <= limitPx) {
            out[0] = x
            out[1] = y
            return
        }
        if (rubberBandPx <= 0f) {
            clamp(x, y, out)
            return
        }
        val overshoot = distance - limitPx
        val damped = overshoot * rubberBandPx / (overshoot + rubberBandPx)
        val ratio = (limitPx + damped) / distance
        out[0] = x * ratio
        out[1] = y * ratio
    }

    companion object {
        fun forGeometry(geometry: HoneycombGeometry, viewportRadiusPx: Float): PanBounds =
            PanBounds(
                limitPx = geometry.contentRadiusPx,
                rubberBandPx = viewportRadiusPx * HoneycombDefaults.RUBBER_BAND_FRACTION,
            )
    }
}
