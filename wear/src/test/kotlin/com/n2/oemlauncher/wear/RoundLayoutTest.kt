package com.n2.oemlauncher.wear

import com.n2.oemlauncher.wear.ui.RoundEdge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundLayoutTest {
    @Test fun rowsKeepFullSizeInTheMiddleAndShrinkTowardsBothEdges() {
        assertEquals(1f, RoundEdge.falloff(0.5f, 0.7f), 1e-4f)
        assertEquals(1f, RoundEdge.falloff(0.4f, 0.7f, flat = 0.3f), 1e-4f)
        assertEquals(0.7f, RoundEdge.falloff(0f, 0.7f), 1e-4f)
        assertEquals(0.7f, RoundEdge.falloff(1f, 0.7f), 1e-4f)
        assertEquals(0.7f, RoundEdge.falloff(-2f, 0.7f), 1e-4f)
        val near = RoundEdge.falloff(0.9f, 0.7f)
        val nearer = RoundEdge.falloff(0.97f, 0.7f)
        assertTrue(near in 0.7f..1f && nearer < near)
        assertEquals(RoundEdge.falloff(0.2f, 0.7f), RoundEdge.falloff(0.8f, 0.7f), 1e-4f)
    }

    @Test fun arcInsetFollowsTheCircle() {
        assertEquals(0f, RoundEdge.arcInset(0.5f), 1e-4f)
        assertEquals(1f, RoundEdge.arcInset(0f), 1e-4f)
        assertEquals(1f, RoundEdge.arcInset(1.4f), 1e-4f)
        // Halfway between center and edge the circle sits 1 - sqrt(0.75) of the radius in.
        assertEquals(0.1340f, RoundEdge.arcInset(0.25f), 1e-3f)
        assertEquals(RoundEdge.arcInset(0.25f), RoundEdge.arcInset(0.75f), 1e-4f)
    }
}
