package com.n2.oemlauncher.shared.layout

import kotlin.math.hypot

/**
 * Reusable per-frame output so scrolling never allocates. Coordinates are relative to the
 * viewport center. An [alpha] of 0 means off screen: skip drawing and hit testing.
 */
class HoneycombFrame(val capacity: Int) {
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val scale = FloatArray(capacity)
    val alpha = FloatArray(capacity)

    var size: Int = 0
        internal set

    var visibleCount: Int = 0
        internal set
}

/** Projects the geometry through the lens for one pan offset. Allocation-free. */
class HoneycombProjector(
    private val geometry: HoneycombGeometry,
    private val lens: FisheyeLens,
    private val iconRadiusPx: Float,
) {
    fun project(panX: Float, panY: Float, viewportRadiusPx: Float, out: HoneycombFrame) {
        require(out.capacity >= geometry.count) { "frame capacity too small" }
        val radius = viewportRadiusPx.coerceAtLeast(MIN_RADIUS_PX)
        var visible = 0
        for (i in 0 until geometry.count) {
            val px = geometry.baseX[i] + panX
            val py = geometry.baseY[i] + panY
            val distance = hypot(px, py)
            val u = distance / radius
            val displacedU = lens.displacedDistance(u)
            val scale = lens.scaleAt(u)
            val ratio = if (distance > EPSILON) displacedU * radius / distance else 1f
            out.x[i] = px * ratio
            out.y[i] = py * ratio
            out.scale[i] = scale
            val alpha = lens.alphaAt(displacedU + iconRadiusPx * scale / radius)
            out.alpha[i] = alpha
            if (alpha > 0f) visible++
        }
        out.size = geometry.count
        out.visibleCount = visible
    }

    private companion object {
        const val EPSILON = 1e-3f
        const val MIN_RADIUS_PX = 1f
    }
}
