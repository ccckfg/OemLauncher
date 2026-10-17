package com.n2.oemlauncher.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * The phone follows the system theme and never the wallpaper: Mocha in the dark, Latte in the
 * light (design section 2). [darkOverride] is the manual switch in the top bar; null means
 * "follow the system".
 */
@Composable
fun CompanionTheme(darkOverride: Boolean?, content: @Composable () -> Unit) {
    val dark = darkOverride ?: isSystemInDarkTheme()
    val palette = remember(dark) { GlassPalette(PaletteMapping.palette(dark)) }
    val colors = remember(palette, dark) { PaletteMapping.colorScheme(palette.design, dark) }
    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = CompanionTypography, content = content)
    }
}
