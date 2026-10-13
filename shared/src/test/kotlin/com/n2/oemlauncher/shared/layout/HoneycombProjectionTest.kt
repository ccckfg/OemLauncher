package com.n2.oemlauncher.shared.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class HoneycombProjectionTest {

    private val pitch = 60f
    private val iconRadius = 25f
    private val viewportRadius = 227f
    private val geometry = HoneycombGeometry(count = 61, pitchPx = pitch)
    private val projector = HoneycombProjector(geometry, FisheyeLens(0.7f), iconRadius)
    private val frame = HoneycombFrame(geometry.count)

    @Test
    fun centerCellStaysCenteredAndFullSize() {
        projector.project(0f, 0f, viewportRadius, frame)
        assertEquals(0f, frame.x[0], 1e-4f)
        assertEquals(1f, frame.scale[0], 1e-6f)
        assertEquals(1f, frame.alpha[0], 0f)
    }

    @Test
    fun lensPacksNearRingsAndCullsFarOnes() {
        projector.project(0f, 0f, viewportRadius, frame)
        assertEquals("four rings fit thanks to the lens", geometry.count, frame.visibleCount)

        val big = HoneycombGeometry(count = HexSpiral.capacity(8), pitchPx = pitch)
        val bigFrame = HoneycombFrame(big.count)
        HoneycombProjector(big, FisheyeLens(0.7f), iconRadius).project(0f, 0f, viewportRadius, bigFrame)
        assertTrue(bigFrame.visibleCount in geometry.count until big.count)
        assertEquals(0f, bigFrame.alpha[big.count - 1], 0f)
    }

    @Test
    fun hitTestPicksNearestVisibleCell() {
        projector.project(0f, 0f, viewportRadius, frame)
        assertEquals(0, HoneycombHitTest.hit(frame, 0f, 0f, iconRadius, 24f))
        assertEquals(3, HoneycombHitTest.hit(frame, pitch, 0f, iconRadius, 24f))
        assertEquals(3, HoneycombHitTest.hit(frame, pitch * 0.6f, 2f, iconRadius, 24f))
        assertEquals(-1, HoneycombHitTest.hit(frame, 400f, 400f, iconRadius, 24f))
    }

    @Test
    fun panningMovesContent() {
        projector.project(-pitch, 0f, viewportRadius, frame)
        assertEquals(3, HoneycombHitTest.hit(frame, 0f, 0f, iconRadius, 24f))
        assertEquals(1f, frame.scale[3], 1e-6f)
    }

    @Test
    fun snapTargetCentersNearestCell() {
        val out = FloatArray(2)
        val index = geometry.snapTarget(-pitch * 0.9f, 3f, out)
        assertEquals(3, index)
        assertEquals(-pitch, out[0], 1e-4f)
        assertEquals(0f, out[1], 1e-4f)
    }

    @Test
    fun panBoundsClampAndResist() {
        val bounds = PanBounds.forGeometry(geometry, viewportRadius)
        val out = FloatArray(2)
        bounds.clamp(10f, 0f, out)
        assertEquals(10f, out[0], 0f)
        val far = geometry.contentRadiusPx * 3f
        bounds.clamp(far, 0f, out)
        assertEquals(geometry.contentRadiusPx, hypot(out[0], out[1]), 1e-3f)
        bounds.rubberBand(far, 0f, out)
        val resisted = hypot(out[0], out[1])
        assertTrue(resisted > geometry.contentRadiusPx)
        assertTrue(resisted < geometry.contentRadiusPx + viewportRadius * HoneycombDefaults.RUBBER_BAND_FRACTION)
    }

    @Test
    fun singleCellGeometryHasNoPanRoom() {
        val single = HoneycombGeometry(count = 1, pitchPx = pitch)
        val out = FloatArray(2)
        PanBounds.forGeometry(single, viewportRadius).clamp(50f, 50f, out)
        assertEquals(0f, out[0], 0f)
        assertEquals(0f, out[1], 0f)
    }
}
