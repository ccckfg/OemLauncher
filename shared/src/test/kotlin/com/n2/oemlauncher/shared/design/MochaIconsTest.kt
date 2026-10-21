package com.n2.oemlauncher.shared.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MochaIconsTest {

    @Test
    fun everyGlyphIsAnAbsolutePathInsideTheViewport() {
        MochaGlyph.entries.forEach { glyph ->
            // A leading lowercase moveto is absolute too: it starts from the origin.
            assertTrue(glyph.name, glyph.pathData.startsWith("M", ignoreCase = true))
            val numbers = Regex("-?\\d+(\\.\\d+)?").findAll(glyph.pathData.drop(1).substringBefore('q'))
                .map { it.value.toFloat() }.take(2).toList()
            assertTrue(glyph.name, numbers[0] in 0f..MochaIcons.VIEWPORT)
            assertTrue(glyph.name, numbers[1] in -MochaIcons.VIEWPORT..0f)
        }
    }

    @Test
    fun coversTheWatchAppsByPackage() {
        assertEquals(MochaGlyph.FORUM, MochaIcons.forPackage("com.tencent.watchwechat")?.glyph)
        assertEquals(MochaIcons.Tone.TERRACOTTA, MochaIcons.forPackage("com.samsung.android.watch.alarm")?.tone)
        assertNull(MochaIcons.forPackage("com.example.unknown"))
    }

    @Test
    fun toneFollowsHueAndNeutralsTakeClay() {
        assertEquals(MochaIcons.Tone.TERRACOTTA, MochaIcons.toneFor(0xFFE53935.toInt()))
        assertEquals(MochaIcons.Tone.OCHRE, MochaIcons.toneFor(0xFFFFA000.toInt()))
        assertEquals(MochaIcons.Tone.SAGE, MochaIcons.toneFor(0xFF43A047.toInt()))
        assertEquals(MochaIcons.Tone.TEAL, MochaIcons.toneFor(0xFF00ACC1.toInt()))
        assertEquals(MochaIcons.Tone.SLATE, MochaIcons.toneFor(0xFF1E88E5.toInt()))
        assertEquals(MochaIcons.Tone.PLUM, MochaIcons.toneFor(0xFF8E24AA.toInt()))
        assertEquals(MochaIcons.Tone.TERRACOTTA, MochaIcons.toneFor(0xFFD81B60.toInt()))
        assertEquals(MochaIcons.Tone.CLAY, MochaIcons.toneFor(0xFF9E9E9E.toInt()))
        assertEquals(MochaIcons.Tone.CLAY, MochaIcons.toneFor(0xFF000000.toInt()))
    }

    @Test
    fun creamInkKeepsContrastOnEveryTone() {
        MochaIcons.Tone.entries.forEach { tone ->
            val ratio = ColorMath.contrast(MochaIcons.INK, tone.argb)
            assertTrue("${tone.name} $ratio", ratio >= MIN_CONTRAST)
        }
    }

    private companion object {
        const val MIN_CONTRAST = 6.0
    }
}
