package com.n2.oemlauncher.shared.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelZoneTest {
    private val radius = 100f
    private val stripTop = radius * (1f - LabelZone.DEPTH_FRACTION)

    @Test
    fun cellsAboveTheStripStayOpaque() {
        assertEquals(1f, LabelZone.alpha(cellCenterY = 0f, cellRadius = 10f, screenRadius = radius), 0f)
        assertEquals(1f, LabelZone.alpha(stripTop - 10f, 10f, radius), 0f)
        assertEquals(1f, LabelZone.alpha(cellCenterY = -95f, cellRadius = 10f, screenRadius = radius), 0f)
    }

    @Test
    fun cellsFullyInsideTheStripFadeToTheMinimum() {
        assertEquals(LabelZone.MIN_ALPHA, LabelZone.alpha(stripTop + 10f, 10f, radius), 1e-6f)
        assertEquals(LabelZone.MIN_ALPHA, LabelZone.alpha(radius, 10f, radius), 1e-6f)
    }

    @Test
    fun fadeIsHalfwayWhenTheStripCutsThroughTheCellCenter() {
        val expected = 1f - (1f - LabelZone.MIN_ALPHA) / 2f
        assertEquals(expected, LabelZone.alpha(stripTop, 10f, radius), 1e-6f)
    }

    @Test
    fun fadeNeverJumpsWhileACellScrollsDown() {
        var previous = 1f
        var y = 0f
        while (y <= radius) {
            val alpha = LabelZone.alpha(y, 12f, radius)
            assertTrue(alpha <= previous)
            assertTrue(previous - alpha < 0.05f)
            previous = alpha
            y += 0.5f
        }
    }

    @Test
    fun degenerateCellsAreLeftAlone() {
        assertEquals(1f, LabelZone.alpha(radius, 0f, radius), 0f)
    }
}
