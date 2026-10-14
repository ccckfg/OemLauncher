package com.n2.oemlauncher.wear.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class IconCacheKeyTest {
    @Test
    fun keyIncludesContentSizeAndShape() {
        assertEquals(IconCacheKey("abc", 128, "CIRCLE"), IconCacheKey("abc", 128, "CIRCLE"))
        assertEquals(2, setOf(IconCacheKey("abc", 128, "CIRCLE"), IconCacheKey("abc", 192, "CIRCLE")).size)
    }
}
