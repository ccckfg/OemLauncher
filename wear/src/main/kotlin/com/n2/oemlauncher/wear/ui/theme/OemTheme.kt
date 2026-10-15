package com.n2.oemlauncher.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Misty-navy palette: periwinkle accents over deep blue surfaces that fall off to black. */
object OemColors {
    val Primary = Color(0xFFB4C4FF)
    val PrimaryDim = Color(0xFF93A6EE)
    val PrimaryContainer = Color(0xFF34457A)
    val OnPrimary = Color(0xFF13204A)
    val OnPrimaryContainer = Color(0xFFDDE3FF)
    val Secondary = Color(0xFFC6C8E6)
    val SecondaryContainer = Color(0xFF3A3F5E)
    val OnSecondaryContainer = Color(0xFFE1E2F7)
    val Tertiary = Color(0xFFD9BCFF)
    val TertiaryContainer = Color(0xFF4C3B70)
    val SurfaceLow = Color(0xFF151A2B)
    val Surface = Color(0xFF1D2338)
    val SurfaceHigh = Color(0xFF272E47)
    val OnSurface = Color(0xFFE7EAF6)
    val OnSurfaceVariant = Color(0xFFB5BBD2)
    val Outline = Color(0xFF7E859E)
    val OutlineVariant = Color(0xFF3C4359)
    val Error = Color(0xFFFFB4AB)

    val BackdropEdge = Color.Black
    val BackdropCore = Color(0xFF263760)
    val BackdropMist = Color(0xFF3A2E66)
    val BackdropMistAlpha = 0.5f

    val GearDisc = Color(0xFF2A3557)
    val GearRing = Color(0x33FFFFFF)
    val GearGlyph = OnPrimaryContainer
    val Placeholder = Color(0xFF262D45)
    val Scrim = Color.Black
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
