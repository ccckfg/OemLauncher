package com.n2.oemlauncher.wear.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.shared.design.GlassSpecs
import com.n2.oemlauncher.shared.model.VisualTier

/**
 * Misty navy backdrop: a soft glow above the center plus an off-center violet haze, both
 * falling off to black at the bezel. Every screen draws its own copy because swipe-to-dismiss
 * slides the top screen over the one below. Brushes are rebuilt only when the size changes.
 */
fun Modifier.oemBackdrop(tier: VisualTier = VisualTier.REFINED): Modifier = drawWithCache {
    val spec = GlassSpecs.watch(tier)
    val radius = size.minDimension / 2f
    val core = Brush.radialGradient(
        listOf(OemColors.BackdropCore, Color.Transparent),
        center = Offset(size.width / 2f, size.height * 0.42f),
        radius = radius * 1.1f,
    )
    val mist = Brush.radialGradient(
        listOf(OemColors.BackdropMist.copy(alpha = OemColors.BackdropMistAlpha), Color.Transparent),
        center = Offset(size.width * 0.7f, size.height * 0.72f),
        radius = radius * 0.85f,
    )
    onDrawBehind {
        drawRect(OemColors.Background)
        if (spec.ambientGlow) {
            drawRect(core)
            drawRect(mist)
        }
    }
}

/** Darkens content near the bezel so icons sink into the edge instead of being cut by it. */
fun Modifier.edgeVignette(tier: VisualTier = VisualTier.REFINED): Modifier = drawWithCache {
    val spec = GlassSpecs.watch(tier)
    if (spec.ambientGlow) {
        val brush = Brush.radialGradient(
            0f to Color.Transparent,
            WearUiTokens.VIGNETTE_START_FRACTION to Color.Transparent,
            1f to OemColors.Background.copy(alpha = WearUiTokens.VIGNETTE_ALPHA),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension / 2f,
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush)
        }
    } else {
        onDrawWithContent { drawContent() }
    }
}
