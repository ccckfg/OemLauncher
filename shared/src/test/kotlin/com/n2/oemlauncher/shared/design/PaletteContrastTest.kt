package com.n2.oemlauncher.shared.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {

    private val palettes = mapOf("Mocha" to Palettes.Mocha, "Latte" to Palettes.Latte)

    private fun assertContrast(name: String, fg: Long, bg: Long, min: Double) {
        val ratio = ColorMath.contrast(fg, bg)
        assertTrue("$name: %.2f < %.1f".format(ratio, min), ratio >= min)
    }

    @Test
    fun bodyTextIsReadableOnBackgroundAndGlass() {
        palettes.forEach { (name, p) ->
            val glass = ColorMath.compositeOver(p.glass, p.background)
            val roles = listOf("primary" to p.textPrimary, "secondary" to p.textSecondary, "accentText" to p.accentText)
            for ((role, text) in roles) {
                assertContrast("$name $role on background", text, p.background, 4.5)
                assertContrast("$name $role on glass", text, glass, 4.5)
            }
            assertContrast("$name onAccent", p.onAccent, p.accent, 4.5)
            assertContrast("$name onAccent on light accent", p.onAccent, p.accentLight, 4.5)
        }
    }

    @Test
    fun hintsAndIndicatorsMeetNonTextContrast() {
        palettes.forEach { (name, p) ->
            val glass = ColorMath.compositeOver(p.glass, p.background)
            assertContrast("$name tertiary on background", p.textTertiary, p.background, 3.0)
            assertContrast("$name tertiary on glass", p.textTertiary, glass, 3.0)
            assertContrast("$name highlight", p.highlight, p.background, 3.0)
            assertContrast("$name negative", p.negative, glass, 3.0)
        }
    }

    @Test
    fun onlyGlassRolesAreTranslucent() {
        palettes.forEach { (name, p) ->
            assertTrue("$name glass", ColorMath.alpha(p.glass) < 1f)
            assertTrue("$name glassStrong", ColorMath.alpha(p.glassStrong) < 1f)
            assertTrue("$name rimLight", ColorMath.alpha(p.rimLight) < 1f)
            listOf(p.background, p.accent, p.onAccent, p.textPrimary, p.textSecondary).forEach {
                assertEquals("$name opaque role", 1f, ColorMath.alpha(it))
            }
        }
    }

    @Test
    fun compositeOfOpaqueColorIsTheColorItself() {
        assertEquals(0xFF123456, ColorMath.compositeOver(0xFF123456, 0xFFFFFFFF))
        assertEquals(0xFF808080, ColorMath.compositeOver(0x80FFFFFF, 0xFF000000))
        assertEquals(21.0, ColorMath.contrast(0xFF000000, 0xFFFFFFFF), 0.01)
    }
}
