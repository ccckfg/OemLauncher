package com.n2.oemlauncher.wear.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IconCacheKeyTest {
    @Test
    fun keyIncludesContentSizeAndShape() {
        assertEquals(IconCacheKey("abc", 128, "CIRCLE"), IconCacheKey("abc", 128, "CIRCLE"))
        assertEquals(2, setOf(IconCacheKey("abc", 128, "CIRCLE"), IconCacheKey("abc", 192, "CIRCLE")).size)
    }

    @Test fun themedCacheSeparatesAppSizeShapeAndStyle() {
        val themed = IconCacheKey("clock/Clock", 64, "CIRCLE", "THEMED")
        assertEquals(themed, themed.copy())
        assertNotEquals(themed, themed.copy(iconId = "contacts/Contacts"))
        assertNotEquals(themed, themed.copy(sizePx = 96))
        assertNotEquals(themed, themed.copy(shape = "SQUIRCLE"))
        assertNotEquals(themed, themed.copy(style = "ORIGINAL"))
    }
}
