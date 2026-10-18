package com.n2.oemlauncher.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.n2.oemlauncher.shared.design.Palettes

private fun mochaColor(value: Long): Color = Color(value)

/** Mocha is fixed on Wear so the watch and the phone preview share exact color roles. */
object OemColors {
    val Background = mochaColor(Palettes.Mocha.background)
    val BackgroundGlow = mochaColor(Palettes.Mocha.backgroundGlow)
    val CoolGlow = mochaColor(Palettes.Mocha.coolGlow)
    val Glass = mochaColor(Palettes.Mocha.glass)
    val GlassStrong = mochaColor(Palettes.Mocha.glassStrong)
    val RimLight = mochaColor(Palettes.Mocha.rimLight)
    val Accent = mochaColor(Palettes.Mocha.accent)
    val AccentLight = mochaColor(Palettes.Mocha.accentLight)
    val AccentDeep = mochaColor(Palettes.Mocha.accentDeep)
    val OnAccent = mochaColor(Palettes.Mocha.onAccent)
    val AccentText = mochaColor(Palettes.Mocha.accentText)
    val TextPrimary = mochaColor(Palettes.Mocha.textPrimary)
    val TextSecondary = mochaColor(Palettes.Mocha.textSecondary)
    val TextTertiary = mochaColor(Palettes.Mocha.textTertiary)
    val Highlight = mochaColor(Palettes.Mocha.highlight)
    val Positive = mochaColor(Palettes.Mocha.positive)
    val Negative = mochaColor(Palettes.Mocha.negative)

    val Primary = Accent
    val PrimaryDim = AccentDeep
    val PrimaryContainer = GlassStrong
    val OnPrimary = OnAccent
    val OnPrimaryContainer = TextPrimary
    val Secondary = TextSecondary
    val SecondaryContainer = Glass
    val OnSecondaryContainer = TextPrimary
    val Tertiary = Highlight
    val TertiaryContainer = GlassStrong
    val SurfaceLow = Background
    val Surface = Glass
    val SurfaceHigh = GlassStrong
    val OnSurface = TextPrimary
    val OnSurfaceVariant = TextSecondary
    val Outline = RimLight
    val OutlineVariant = GlassStrong
    val Error = Negative

    val BackdropEdge = Background
    val BackdropCore = BackgroundGlow
    val BackdropMist = CoolGlow
    val BackdropMistAlpha = 0.75f
    val GearDisc = GlassStrong
    val GearRing = RimLight
    val GearGlyph = OnAccent
    val Placeholder = GlassStrong
    val Scrim = Background
}

private val oemColorScheme = ColorScheme(
    primary = OemColors.Primary,
    primaryDim = OemColors.PrimaryDim,
    primaryContainer = OemColors.PrimaryContainer,
    onPrimary = OemColors.OnPrimary,
    onPrimaryContainer = OemColors.OnPrimaryContainer,
    secondary = OemColors.Secondary,
    secondaryContainer = OemColors.SecondaryContainer,
    onSecondaryContainer = OemColors.OnSecondaryContainer,
    tertiary = OemColors.Tertiary,
    tertiaryContainer = OemColors.TertiaryContainer,
    surfaceContainerLow = OemColors.SurfaceLow,
    surfaceContainer = OemColors.Surface,
    surfaceContainerHigh = OemColors.SurfaceHigh,
    onSurface = OemColors.OnSurface,
    onSurfaceVariant = OemColors.OnSurfaceVariant,
    outline = OemColors.Outline,
    outlineVariant = OemColors.OutlineVariant,
    background = OemColors.BackdropEdge,
    onBackground = OemColors.OnSurface,
    error = OemColors.Error,
)

@Composable
fun OemTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = oemColorScheme, content = content)
}
