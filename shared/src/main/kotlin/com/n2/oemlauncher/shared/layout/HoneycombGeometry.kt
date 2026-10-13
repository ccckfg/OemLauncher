package com.n2.oemlauncher.shared.layout

import kotlin.math.hypot
import kotlin.math.max

/**
 * Unscaled honeycomb cell centers in pixels, cell 0 at the origin. Build once per
 * (app count, icon size) and reuse it for every frame.
 */
class HoneycombGeometry(val count: Int, val pitchPx: Float) {
    val baseX = FloatArray(count)
    val baseY = FloatArray(count)

    /** Distance from the origin to the farthest cell center. */
    val contentRadiusPx: Float

    init {
        require(count >= 0) { "count must be >= 0" }
        require(pitchPx > 0f) { "pitchPx must be > 0" }
        HexSpiral.positions(count, baseX, baseY)
        var farthest = 0f
        for (i in 0 until count) {
            baseX[i] *= pitchPx
            baseY[i] *= pitchPx
            farthest = max(farthest, hypot(baseX[i], baseY[i]))
        }
        contentRadiusPx = farthest
    }

    /** Index of the cell whose unscaled center is closest to ([x], [y]); -1 when empty. */
    fun nearestCell(x: Float, y: Float): Int {
        var best = -1
        var bestDistance = Float.MAX_VALUE
        for (i in 0 until count) {
            val dx = baseX[i] - x
            val dy = baseY[i] - y
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = i
            }
        }
        return best
    }

    /**
     * Pan offset that puts the cell nearest to the current center exactly in the center.
     * Writes the offset into [out] (size 2) and returns the cell index, or -1 when empty.
     */
    fun snapTarget(panX: Float, panY: Float, out: FloatArray): Int {
        val index = nearestCell(-panX, -panY)
        if (index < 0) {
            out[0] = 0f
            out[1] = 0f
        } else {
            out[0] = -baseX[index]
            out[1] = -baseY[index]
        }
        return index
    }

    /** Pan offset that centers the cell at [index]. */
    fun panToCenter(index: Int, out: FloatArray) {
        require(index in 0 until count) { "index out of range: $index" }
        out[0] = -baseX[index]
        out[1] = -baseY[index]
    }
}
