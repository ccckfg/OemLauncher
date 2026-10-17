package com.n2.oemlauncher.mobile.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.n2.oemlauncher.shared.design.ColorMath
import com.n2.oemlauncher.shared.design.DesignPalette
import com.n2.oemlauncher.shared.design.Palettes

/**
 * Maps a mocha glass palette onto the Material3 roles. A pure function of the palette, so the
 * mapping is unit-testable and no wall paper color ever leaks in (design section 2).
 */
object PaletteMapping {
    /** The phone follows the system: Mocha in the dark, Latte in the light. */
    fun palette(dark: Boolean): DesignPalette = if (dark) Palettes.Mocha else Palettes.Latte

    fun colorScheme(palette: DesignPalette, dark: Boolean): ColorScheme {
        val background = Color(palette.background)
        val surface = Color(composite(palette.glass, palette))
        val surfaceStrong = Color(composite(palette.glassStrong, palette))
        val primary = Color(palette.accent)
        val builder = if (dark) darkColorScheme() else lightColorScheme()
        return builder.copy(
            primary = primary,
            onPrimary = Color(palette.onAccent),
            primaryContainer = Color(palette.accentDeep),
            onPrimaryContainer = Color(palette.onAccent),
            inversePrimary = Color(palette.accentDeep),
            secondary = Color(palette.accentText),
            onSecondary = Color(palette.onAccent),
            secondaryContainer = surfaceStrong,
            onSecondaryContainer = Color(palette.textPrimary),
            tertiary = Color(palette.highlight),
            onTertiary = Color(palette.onAccent),
            tertiaryContainer = surface,
            onTertiaryContainer = Color(palette.textPrimary),
            background = background,
            onBackground = Color(palette.textPrimary),
            surface = surface,
            onSurface = Color(palette.textPrimary),
            surfaceVariant = surfaceStrong,
            onSurfaceVariant = Color(palette.textSecondary),
            surfaceTint = primary,
            inverseSurface = Color(palette.textPrimary),
            inverseOnSurface = background,
            error = Color(palette.negative),
            onError = Color(palette.onAccent),
            errorContainer = surfaceStrong,
            onErrorContainer = Color(palette.negative),
            outline = Color(palette.textTertiary),
            outlineVariant = surface,
            scrim = Color.Black,
            surfaceBright = surfaceStrong,
            surfaceDim = background,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceStrong,
            surfaceContainerHighest = surfaceStrong,
            surfaceContainerLow = surface,
            surfaceContainerLowest = background,
        )
    }

    private fun composite(glass: Long, palette: DesignPalette): Long =
        ColorMath.compositeOver(glass, palette.background)
}
