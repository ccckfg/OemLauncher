package com.n2.oemlauncher.shared.design

import com.n2.oemlauncher.shared.model.VisualTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class CookieOutlineTest {

    @Test
    fun radiusSwingsBetweenTheScallopDepthAndTheFullRadius() {
        val points = CookieOutline.points(radius = 100f)
        val radii = (points.indices step 2).map { hypot(points[it], points[it + 1]) }
        assertEquals(100f, radii.max(), 0.01f)
        assertEquals(100f * (1 - CookieOutline.DEPTH), radii.min(), 0.5f)
    }

    @Test
    fun startsWithABumpPointingUp() {
        val points = CookieOutline.points(radius = 50f)
        assertEquals(0f, points[0], 0.001f)
        assertEquals(-50f, points[1], 0.001f)
    }

    @Test
    fun pointCountScalesWithScallops() {
        assertEquals(CookieOutline.points(10f, scallops = 9).size * 2, CookieOutline.points(10f, scallops = 18).size)
    }

    @Test
    fun liteTierDropsEveryCostlyEffect() {
        val lite = GlassSpecs.watch(VisualTier.LITE)
        assertEquals(0f, lite.overlayBlurDp)
        assertEquals(0f, lite.shadowAlpha)
        assertFalse(lite.ambientGlow)
        assertFalse(lite.animated)
        assertTrue(GlassSpecs.watch(VisualTier.REFINED).animated)
    }
}
