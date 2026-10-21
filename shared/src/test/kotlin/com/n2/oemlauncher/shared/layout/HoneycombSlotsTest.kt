package com.n2.oemlauncher.shared.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoneycombSlotsTest {
    @Test
    fun gearTakesTheFirstCellAndAppsFollowInOrder() {
        assertEquals(1, HoneycombSlots.cellCount(0))
        assertEquals(8, HoneycombSlots.cellCount(7))
        assertTrue(HoneycombSlots.isSettings(0))
        assertFalse(HoneycombSlots.isSettings(1))
        assertEquals(-1, HoneycombSlots.appAt(0))
        assertEquals(0, HoneycombSlots.appAt(1))
        assertEquals(6, HoneycombSlots.appAt(7))
        assertEquals(-1, HoneycombSlots.appAt(-3))
    }
}
