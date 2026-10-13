package com.n2.oemlauncher.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.n2.oemlauncher.mobile.config.ThemeTokens

private val Light = lightColorScheme(
    primary = Color(ThemeTokens.PRIMARY_LIGHT), onPrimary = Color.White,
    background = Color(ThemeTokens.SURFACE_LIGHT), surface = Color(ThemeTokens.SURFACE_LIGHT),
    onBackground = Color(ThemeTokens.TEXT_LIGHT), onSurface = Color(ThemeTokens.TEXT_LIGHT),
)
private val Dark = darkColorScheme(
    primary = Color(ThemeTokens.PRIMARY_DARK), background = Color(ThemeTokens.SURFACE_DARK),
    surface = Color(ThemeTokens.SURFACE_DARK), onSurface = Color(ThemeTokens.TEXT_DARK),
)

@Composable
fun CompanionTheme(darkOverride: Boolean?, content: @Composable () -> Unit) {
    val dark = darkOverride ?: isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) Dark else Light
    MaterialTheme(colorScheme = colors, content = content)
}
