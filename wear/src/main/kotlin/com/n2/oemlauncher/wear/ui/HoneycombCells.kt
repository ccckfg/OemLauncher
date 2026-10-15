package com.n2.oemlauncher.wear.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.curvedText
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.theme.OemColors
import kotlin.math.roundToInt

/** Gear resources prepared once, so drawing the gear cell allocates nothing per frame. */
class GearGlyph(val painter: VectorPainter, val tint: ColorFilter, val ring: Stroke)

@Composable
fun rememberGearGlyph(): GearGlyph {
    val painter = rememberVectorPainter(Icons.Rounded.Settings)
    val ringPx = with(LocalDensity.current) { WearUiTokens.GEAR_RING_DP.dp.toPx() }
    return remember(painter, ringPx) { GearGlyph(painter, ColorFilter.tint(OemColors.GearGlyph), Stroke(ringPx)) }
}

internal fun DrawScope.drawGearCell(glyph: GearGlyph, x: Float, y: Float, radius: Float, alpha: Float) {
    val center = Offset(x, y)
    drawCircle(OemColors.GearDisc, radius, center, alpha)
    drawCircle(OemColors.GearRing, radius - glyph.ring.width / 2f, center, alpha, style = glyph.ring)
    val edge = radius * 2f * WearUiTokens.GEAR_GLYPH_FRACTION
    translate(x - edge / 2f, y - edge / 2f) {
        with(glyph.painter) { draw(Size(edge, edge), alpha, glyph.tint) }
    }
}

internal fun DrawScope.drawAppCell(icon: ImageBitmap?, x: Float, y: Float, radius: Float, alpha: Float) {
    if (icon == null) {
        drawCircle(OemColors.Placeholder, radius, Offset(x, y), alpha)
        return
    }
    val diameter = (radius * 2f).roundToInt().coerceAtLeast(1)
    drawImage(
        icon,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(icon.width, icon.height),
        dstOffset = IntOffset((x - radius).roundToInt(), (y - radius).roundToInt()),
        dstSize = IntSize(diameter, diameter),
        alpha = alpha,
        filterQuality = FilterQuality.Medium,
    )
}

/** Name of the centered cell, curved along the bottom bezel; it fades out while dragging. */
@Composable
fun HoneycombFocusLabel(text: String, visible: Boolean) {
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(WearUiTokens.FOCUS_LABEL_FADE_MS),
        label = "focusLabel",
    )
    val color = MaterialTheme.colorScheme.onSurface
    CurvedLayout(
        // Decorative: every cell already has its own accessibility node with the same name.
        Modifier.fillMaxSize().clearAndSetSemantics {}.graphicsLayer { this.alpha = alpha },
        anchor = 90f,
        angularDirection = CurvedDirection.Angular.CounterClockwise,
    ) {
        curvedText(
            text,
            maxSweepAngle = WearUiTokens.FOCUS_LABEL_SWEEP_DEGREES,
            fontSize = WearUiTokens.FOCUS_LABEL_SP.sp,
            color = color,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
