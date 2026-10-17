package com.n2.oemlauncher.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.n2.oemlauncher.shared.design.ColorMath
import com.n2.oemlauncher.shared.design.DesignPalette
import com.n2.oemlauncher.shared.design.Palettes

/**
 * The mocha glass roles that Material3 has no slot for: glass fills, the rim light and the
 * accent tints. Built from the palette in `shared` so the phone and the watch share one source.
 */
@Immutable
class GlassPalette(val design: DesignPalette) {
    val background = Color(design.background)
    val warmGlow = Color(design.backgroundGlow)
    val coolGlow = Color(design.coolGlow)
    val glass = Color(design.glass)
    val glassStrong = Color(design.glassStrong)
    val rimLight = Color(design.rimLight)
    val accent = Color(design.accent)
    val accentLight = Color(design.accentLight)
    val accentDeep = Color(design.accentDeep)
    val onAccent = Color(design.onAccent)
    val accentText = Color(design.accentText)
    val textPrimary = Color(design.textPrimary)
    val textSecondary = Color(design.textSecondary)
    val textTertiary = Color(design.textTertiary)
    val highlight = Color(design.highlight)
    val positive = Color(design.positive)
    val negative = Color(design.negative)

    /** What a pane of glass shows once it lies on the page background. */
    val glassOverBackground = Color(ColorMath.compositeOver(design.glass, design.background))
    val strongOverBackground = Color(ColorMath.compositeOver(design.glassStrong, design.background))
}

val LocalGlassPalette = staticCompositionLocalOf { GlassPalette(Palettes.Mocha) }

/** Reads the active palette, like `MaterialTheme.colorScheme` for the glass roles. */
object GlassTheme {
    val palette: GlassPalette
        @Composable @ReadOnlyComposable get() = LocalGlassPalette.current
}
