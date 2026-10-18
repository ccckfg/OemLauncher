package com.n2.oemlauncher.wear.ui

import com.n2.oemlauncher.shared.design.LabelZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HoneycombCellAlphaTest {
    @Test fun labelZoneMultipliesRatherThanReplacesEdgeFade() {
        val edge = 0.6f
        assertEquals(edge * LabelZone.alpha(90f, 10f, 100f), honeycombCellAlpha(edge, 90f, 10f, 100f, true), 0f)
    }

    @Test fun hiddenNamesDoNotFadeCells() {
        assertEquals(0.6f, honeycombCellAlpha(0.6f, 100f, 10f, 100f, false), 0f)
    }

    @Test fun cellsAboveTheZoneStayUnchanged() {
        assertEquals(0.8f, honeycombCellAlpha(0.8f, 0f, 10f, 100f, true), 0f)
    }

    @Test fun fullyEnteredCellsUseSharedMinimum() {
        assertEquals(LabelZone.MIN_ALPHA, honeycombCellAlpha(1f, 100f, 10f, 100f, true), 0.00001f)
    }

    @Test fun edgeScaledRadiusControlsWhenTheFadeStarts() {
        val small = honeycombCellAlpha(1f, 65f, 5f, 100f, true)
        val large = honeycombCellAlpha(1f, 65f, 30f, 100f, true)
        assertEquals(1f, small, 0f)
        assertTrue(large < small)
    }
}
