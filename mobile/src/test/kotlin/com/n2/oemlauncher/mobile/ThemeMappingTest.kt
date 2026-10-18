package com.n2.oemlauncher.mobile

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.n2.oemlauncher.mobile.ui.theme.GlassPalette
import com.n2.oemlauncher.mobile.ui.theme.PaletteMapping
import com.n2.oemlauncher.shared.design.ColorMath
import com.n2.oemlauncher.shared.design.Palettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeMappingTest {
    private val mocha = PaletteMapping.colorScheme(Palettes.Mocha, dark = true)
    private val latte = PaletteMapping.colorScheme(Palettes.Latte, dark = false)

    @Test fun darkThemeUsesMochaAndLightThemeUsesLatte() {
        assertEquals(Palettes.Mocha, PaletteMapping.palette(dark = true))
        assertEquals(Palettes.Latte, PaletteMapping.palette(dark = false))
        assertEquals(Color(0xFF140E0B), mocha.background)
        assertEquals(Color(0xFFF3EBE2), latte.background)
    }

    @Test fun champagneIsThePrimaryRole() {
        assertEquals(Color(0xFFD6BFA8), mocha.primary)
        assertEquals(Color(0xFFD2B79C), latte.primary)
        assertEquals(Color(0xFF2E1D0D), mocha.onPrimary)
        assertEquals(Color(0xFF2E1D0D), latte.onPrimary)
    }

    @Test fun surfacesAreGlassFlattenedOverTheBackground() {
        val mochaSurface = ColorMath.compositeOver(Palettes.Mocha.glass, Palettes.Mocha.background)
        assertEquals(Color(mochaSurface), mocha.surface)
        val latteSurface = ColorMath.compositeOver(Palettes.Latte.glass, Palettes.Latte.background)
        assertEquals(Color(latteSurface), latte.surface)
        assertTrue(mocha.surface != mocha.background)
        assertTrue(latte.surface != latte.background)
    }

    @Test fun textAndStatusRolesFollowThePalette() {
        assertEquals(Color(0xFFFFF7EE), mocha.onSurface)
        assertEquals(Color(0xFFD9CCC0), mocha.onSurfaceVariant)
        assertEquals(Color(0xFFA8998C), mocha.outline)
        assertEquals(Color(0xFFE8846B), mocha.error)
        assertEquals(Color(0xFFF8BA63), mocha.tertiary)
        assertEquals(Color(0xFF2A1D14), latte.onBackground)
        assertEquals(Color(0xFF5E4E43), latte.onSurfaceVariant)
        assertEquals(Color(0xFFA9482F), latte.error)
    }

    @Test fun schemesKeepTheirOwnBrightness() {
        assertTrue(mocha.background.luminance() < 0.5f)
        assertTrue(latte.background.luminance() > 0.5f)
        assertTrue(latte.onBackground.luminance() < 0.5f)
        assertTrue(mocha.onBackground.luminance() > 0.5f)
    }

    @Test fun glassPaletteExposesTheSharedRolesWithoutCopyingThem() {
        val dark = GlassPalette(Palettes.Mocha)
        assertEquals(Color(0xFFD6BFA8), dark.accent)
        assertEquals(Color(0xFFE6D2BE), dark.accentLight)
        assertEquals(Color(0xFFBEA591), dark.accentDeep)
        assertEquals(Color(0xFF2E1D0D), dark.onAccent)
        assertEquals(Color(0xFFB9C98A), dark.positive)
        assertEquals(Color(0xFFE8846B), dark.negative)
        val light = GlassPalette(Palettes.Latte)
        assertEquals(Color(0xFFD2B79C), light.accent)
        assertEquals(Color(0xFF7E624A), light.accentText)
        assertEquals(Color(0xFF5E7335), light.positive)
        assertEquals(Color(0xB3FFFFFF), light.glass)
    }

    @Test fun glassPaletteCompositesItsTwoFillsOverThePage() {
        val dark = GlassPalette(Palettes.Mocha)
        assertEquals(Color(ColorMath.compositeOver(Palettes.Mocha.glass, Palettes.Mocha.background)), dark.glassOverBackground)
        assertEquals(
            Color(ColorMath.compositeOver(Palettes.Mocha.glassStrong, Palettes.Mocha.background)),
            dark.strongOverBackground,
        )
    }

    /**
     * Champagne as text is [Palettes.accentText]; the fill accent is only for capsules and
     * tracks. This is the legibility rule that keeps the light theme readable.
     */
    @Test fun champagneTextStaysLegibleOnBothBackgrounds() {
        for (palette in listOf(Palettes.Mocha, Palettes.Latte)) {
            val glass = GlassPalette(palette)
            val text = palette.accentText
            for (surface in listOf(palette.background, ColorMath.compositeOver(palette.glass, palette.background))) {
                val ratio = ColorMath.contrast(text, surface)
                assertTrue("accentText $ratio on surface", ratio >= 4.5)
            }
            assertEquals(Color(palette.accentText), glass.accentText)
        }
    }

    /** Documents why the rule exists: the fill accent fails as text on light glass. */
    @Test fun fillAccentIsNotUsableAsTextOnTheLightTheme() {
        val surface = ColorMath.compositeOver(Palettes.Latte.glass, Palettes.Latte.background)
        assertTrue(ColorMath.contrast(Palettes.Latte.accent, surface) < 4.5)
        assertTrue(ColorMath.contrast(Palettes.Mocha.accent, Palettes.Mocha.background) >= 4.5)
    }
}
