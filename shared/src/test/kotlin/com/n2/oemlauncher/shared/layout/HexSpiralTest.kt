package com.n2.oemlauncher.shared.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.roundToInt

class HexSpiralTest {

    @Test
    fun capacityAndRingsMatchHexRings() {
        assertEquals(1, HexSpiral.capacity(0))
        assertEquals(7, HexSpiral.capacity(1))
        assertEquals(19, HexSpiral.capacity(2))
        assertEquals(0, HexSpiral.ringsFor(1))
        assertEquals(1, HexSpiral.ringsFor(7))
        assertEquals(2, HexSpiral.ringsFor(8))
        assertEquals(0, HexSpiral.ringOf(0))
        assertEquals(1, HexSpiral.ringOf(6))
        assertEquals(2, HexSpiral.ringOf(7))
    }

    @Test
    fun positionsAreUniqueWithCenterFirst() {
        val count = 127
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        HexSpiral.positions(count, xs, ys)
        assertEquals(0f, xs[0], 0f)
        assertEquals(0f, ys[0], 0f)
        val cells = (0 until count).map { (xs[it] * 1000).roundToInt() to (ys[it] * 1000).roundToInt() }
        assertEquals(count, cells.toSet().size)
    }

    @Test
    fun firstRingIsAtUnitDistanceClockwiseFromTopLeft() {
        val xs = FloatArray(7)
        val ys = FloatArray(7)
        HexSpiral.positions(7, xs, ys)
        for (i in 1..6) assertEquals(1f, hypot(xs[i], ys[i]), 1e-4f)
        assertTrue("first ring starts top-left", xs[1] < 0f && ys[1] < 0f)
        assertTrue("then top-right", xs[2] > 0f && ys[2] < 0f)
        assertEquals("then right", 0f, ys[3], 1e-4f)
        assertTrue("then bottom-right", xs[4] > 0f && ys[4] > 0f)
    }

    @Test
    fun ringDistancesGrowOutwards() {
        val count = HexSpiral.capacity(4)
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        HexSpiral.positions(count, xs, ys)
        for (i in 1 until count) {
            val ring = HexSpiral.ringOf(i)
            val distance = hypot(xs[i], ys[i])
            assertTrue("cell $i outside its ring", distance <= ring + 1e-3f)
            assertTrue("cell $i inside its ring", distance >= ring * 0.86f)
        }
    }
}
