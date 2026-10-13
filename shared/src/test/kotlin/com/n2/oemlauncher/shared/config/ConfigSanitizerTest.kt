package com.n2.oemlauncher.shared.config

import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigSanitizerTest {

    private val valid = "com.a/com.a.Main"
    private val validIcon = "0123456789abcdef01234567"

    @Test
    fun canonicalizesDeduplicatesAndDropsInvalidKeys() {
        val config = LauncherConfig(
            order = listOf("com.a/.Main", valid, "broken", "com.b/com.b.Main"),
            favorites = listOf(valid, valid, "com.h/com.h.Main"),
            hidden = listOf("com.h/com.h.Main"),
            iconOverrides = mapOf("com.a/.Main" to validIcon, "com.b/com.b.Main" to "NOT-HEX", "bad" to validIcon),
        )
        val clean = ConfigSanitizer.sanitize(config)
        assertEquals(listOf(valid, "com.b/com.b.Main"), clean.order)
        assertEquals(listOf(valid), clean.favorites)
        assertEquals(mapOf(valid to validIcon), clean.iconOverrides)
    }

    @Test
    fun clampsNumbersAndCapsLists() {
        val many = (0 until LauncherDefaults.MAX_FAVORITES + 5).map { "com.x$it/com.x$it.Main" }
        val config = LauncherConfig(
            revision = -4,
            favorites = many,
            layout = LayoutSettings(fisheyeStrength = Float.NaN, gridColumns = 0),
        )
        val clean = ConfigSanitizer.sanitize(config)
        assertEquals(0L, clean.revision)
        assertEquals(LauncherDefaults.MAX_FAVORITES, clean.favorites.size)
        assertEquals(LauncherDefaults.FISHEYE_DEFAULT, clean.layout.fisheyeStrength, 0f)
        assertEquals(LauncherDefaults.GRID_COLUMNS_MIN, clean.layout.gridColumns)
    }

    @Test
    fun catalogSanitizerDropsDuplicatesAndBadIcons() {
        val catalog = WatchCatalog(
            generatedAtMillis = 1,
            deviceName = "Watch",
            sdkInt = 36,
            appVersionName = "1.0.0",
            display = WatchDisplay(454, 454, 320, true),
            entries = listOf(
                CatalogEntry(key = "com.a/.Main", label = "A", iconId = validIcon),
                CatalogEntry(key = valid, label = "A again"),
                CatalogEntry(key = "broken", label = "B"),
                CatalogEntry(key = "com.c/com.c.Main", label = "C".repeat(500), iconId = "zz", launchCount = -1),
            ),
        )
        val clean = CatalogSanitizer.sanitize(catalog)
        assertEquals(listOf(valid, "com.c/com.c.Main"), clean.entries.map { it.key })
        assertEquals(validIcon, clean.entries[0].iconId)
        assertEquals(null, clean.entries[1].iconId)
        assertEquals(LauncherDefaults.MAX_LABEL_LENGTH, clean.entries[1].label.length)
        assertEquals(0, clean.entries[1].launchCount)
    }
}
