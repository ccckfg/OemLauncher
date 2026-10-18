package com.n2.oemlauncher.wear.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.shared.design.GlassSpecs
import com.n2.oemlauncher.shared.model.VisualTier
import kotlin.math.ceil

/**
 * Mocha backdrop with warm and cool glows. Every screen draws its own copy because swipe-to-dismiss
 * slides the top screen over the one below. The refined glows are static, so they are baked into
 * one bitmap per size: the watch GPU then fills the screen once per frame instead of three times.
 */
fun Modifier.oemBackdrop(tier: VisualTier = VisualTier.REFINED): Modifier = drawWithCache {
    val spec = GlassSpecs.watch(tier)
    val width = ceil(size.width).toInt()
    val height = ceil(size.height).toInt()
    if (spec.ambientGlow && width > 0 && height > 0) {
        val radius = size.minDimension / 2f
        val core = Brush.radialGradient(
            listOf(OemColors.BackdropCore, Color.Transparent),
            center = Offset(size.width / 2f, size.height * WearUiTokens.BACKDROP_CORE_Y),
            radius = radius * WearUiTokens.BACKDROP_CORE_RADIUS,
        )
        val mist = Brush.radialGradient(
            listOf(OemColors.BackdropMist.copy(alpha = OemColors.BackdropMistAlpha), Color.Transparent),
            center = Offset(size.width * WearUiTokens.BACKDROP_MIST_X, size.height * WearUiTokens.BACKDROP_MIST_Y),
            radius = radius * WearUiTokens.BACKDROP_MIST_RADIUS,
        )
        val baked = ImageBitmap(width, height)
        CanvasDrawScope().draw(this, layoutDirection, Canvas(baked), size) {
            drawRect(OemColors.Background)
            drawRect(core)
            drawRect(mist)
        }
        onDrawBehind { drawImage(baked) }
    } else onDrawBehind { drawRect(OemColors.Background) }
}

/**
 * Darkens content near the bezel so icons sink into the edge instead of being cut by it. Only the
 * outer ring is painted; covering the whole screen would shade the transparent center every frame.
 */
fun Modifier.edgeVignette(tier: VisualTier = VisualTier.REFINED): Modifier = drawWithCache {
    val spec = GlassSpecs.watch(tier)
    if (spec.ambientGlow) {
        val radius = size.minDimension / 2f
        val inner = radius * WearUiTokens.VIGNETTE_START_FRACTION
        val brush = Brush.radialGradient(
            0f to Color.Transparent,
            WearUiTokens.VIGNETTE_START_FRACTION to Color.Transparent,
            1f to OemColors.Background.copy(alpha = WearUiTokens.VIGNETTE_ALPHA),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = radius,
        )
        val ring = Stroke(radius - inner)
        val ringRadius = (radius + inner) / 2f
        onDrawWithContent {
            drawContent()
            drawCircle(brush, radius = ringRadius, style = ring)
        }
    } else {
        onDrawWithContent { drawContent() }
    }
}
