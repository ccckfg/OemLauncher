package com.n2.oemlauncher.shared.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FisheyeLensTest {

    private val samples = (0..150).map { it / 100f }

    @Test
    fun zeroStrengthIsIdentity() {
        val lens = FisheyeLens(strength = 0f)
        samples.forEach { u ->
            assertEquals(1f, lens.scaleAt(u), 1e-6f)
            assertEquals(u, lens.displacedDistance(u), 1e-5f)
        }
    }

    @Test
    fun flatZoneKeepsFullSize() {
        val lens = FisheyeLens(strength = 1f)
        assertEquals(1f, lens.scaleAt(0f), 0f)
        assertEquals(1f, lens.scaleAt(HoneycombDefaults.FLAT_ZONE), 1e-6f)
        assertEquals(0.2f, lens.displacedDistance(0.2f), 1e-6f)
    }

    @Test
    fun displacementIsStrictlyIncreasingAndScaleNeverGrows() {
        val lens = FisheyeLens(strength = 0.8f)
        for (i in 1 until samples.size) {
            val a = samples[i - 1]
            val b = samples[i]
            assertTrue("displacement must increase", lens.displacedDistance(b) > lens.displacedDistance(a))
            assertTrue("scale must not grow", lens.scaleAt(b) <= lens.scaleAt(a) + 1e-6f)
            assertTrue("scale floor", lens.scaleAt(b) >= lens.minScale - 1e-6f)
        }
    }

    @Test
    fun strongerLensPullsTheRimInwards() {
        val weak = FisheyeLens(strength = 0.2f)
        val strong = FisheyeLens(strength = 1f)
        assertTrue(strong.displacedDistance(1f) < weak.displacedDistance(1f))
        assertTrue(strong.displacedDistance(1f) < 1f)
        assertEquals(HoneycombDefaults.MIN_SCALE_AT_FULL_STRENGTH, strong.minScale, 1e-6f)
    }

    @Test
    fun alphaFadesAcrossTheRim() {
        val lens = FisheyeLens(strength = 0.5f)
        assertEquals(1f, lens.alphaAt(HoneycombDefaults.FADE_START), 0f)
        assertEquals(0f, lens.alphaAt(HoneycombDefaults.FADE_END), 0f)
        val mid = (HoneycombDefaults.FADE_START + HoneycombDefaults.FADE_END) / 2f
        assertEquals(0.5f, lens.alphaAt(mid), 1e-3f)
    }
}
