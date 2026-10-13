package com.n2.oemlauncher.shared.layout

import kotlin.math.sqrt

/**
 * Honeycomb cells in spiral order: index 0 at the center, then ring after ring, each ring
 * clockwise from its top-left cell. Favorites therefore fill the center and its first ring.
 *
 * Positions use unit pitch (neighbor distance = 1) on a pointy-top hex lattice, in screen axes
 * (y grows downwards).
 */
object HexSpiral {
    private const val SIDES = 6
    private val STEP_Q = intArrayOf(1, 0, -1, -1, 0, 1)
    private val STEP_R = intArrayOf(0, 1, 1, 0, -1, -1)
    private val ROW_HEIGHT = (sqrt(3.0) / 2.0).toFloat()

    /** Number of cells in rings 0..[rings]. */
    fun capacity(rings: Int): Int = 1 + 3 * rings * (rings + 1)

    /** Ring that holds the cell at [index]. */
    fun ringOf(index: Int): Int {
        require(index >= 0) { "index must be >= 0" }
        var ring = 0
        while (capacity(ring) <= index) ring++
        return ring
    }

    /** Rings needed to hold [count] cells. */
    fun ringsFor(count: Int): Int = if (count <= 1) 0 else ringOf(count - 1)

    /** Writes the unit-pitch centers of the first [count] cells into [outX] and [outY]. */
    fun positions(count: Int, outX: FloatArray, outY: FloatArray) {
        require(count >= 0 && outX.size >= count && outY.size >= count) { "output too small" }
        if (count == 0) return
        outX[0] = 0f
        outY[0] = 0f
        var index = 1
        var ring = 1
        while (index < count) {
            var q = 0
            var r = -ring
            for (side in 0 until SIDES) {
                repeat(ring) {
                    if (index >= count) return
                    outX[index] = q + r * 0.5f
                    outY[index] = r * ROW_HEIGHT
                    index++
                    q += STEP_Q[side]
                    r += STEP_R[side]
                }
            }
            ring++
        }
    }
}
