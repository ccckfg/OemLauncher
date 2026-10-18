package com.n2.oemlauncher.mobile.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.n2.oemlauncher.mobile.config.ThemeTokens

@Composable
fun MatchSystemBars() {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return
    val light = GlassTheme.palette.background.luminance() > ThemeTokens.LIGHT_SURFACE_THRESHOLD
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }
}
