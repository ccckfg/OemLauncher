package com.n2.oemlauncher.wear.ui

import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.HoneycombProjector
import com.n2.oemlauncher.shared.layout.FisheyeLens
import org.junit.Assert.assertEquals
import org.junit.Test

class HoneycombInteractionTest {
    @Test
    fun nearestVisibleCellWinsForNonCenterTap() {
        val frame = HoneycombFrame(3)
        HoneycombProjector(HoneycombGeometry(3, 80f), FisheyeLens(0f), 20f).project(0f, 0f, 200f, frame)

        assertEquals(1, honeycombHitIndex(frame, frame.x[1], frame.y[1], 20f, 24f))
        assertEquals(-1, honeycombHitIndex(frame, frame.x[2], frame.y[2], 20f, 24f, minAlpha = 1.1f))
    }
}
