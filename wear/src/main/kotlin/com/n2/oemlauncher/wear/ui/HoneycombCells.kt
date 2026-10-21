package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.shared.design.CookieOutline
import com.n2.oemlauncher.shared.design.GlassSpecs
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.theme.OemColors
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The gear is baked into one bitmap when it is remembered. Drawing it as four layers (shadow,
 * fill, ring, glyph) re-tessellated the scalloped path every frame, because the fisheye changes
 * its scale while the honeycomb moves; one bitmap draw per frame keeps the watch GPU free.
 */
class GearGlyph(val image: ImageBitmap, val radius: Float, origin: Float) {
    val dstOffset = IntOffset(-origin.roundToInt(), -origin.roundToInt())
    val dstSize = IntSize(image.width, image.height)
}

@Composable
fun rememberGearGlyph(radiusPx: Float, tier: VisualTier): GearGlyph {
    val painter = rememberVectorPainter(Icons.Rounded.Settings)
    val density = LocalDensity.current
    val ringPx = with(density) { WearUiTokens.GEAR_RING_DP.dp.toPx() }
    return remember(painter, ringPx, radiusPx, tier, density) {
        val pad = ceil(ringPx)
        val drop = radiusPx * WearUiTokens.COOKIE_SHADOW_OFFSET_FRACTION
        val side = ceil(radiusPx * 2f + pad * 2f).toInt()
        val image = ImageBitmap(side, ceil(radiusPx * 2f + drop + pad * 2f).toInt())
        val canvasSize = Size(image.width.toFloat(), image.height.toFloat())
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), canvasSize) {
            translate(pad + radiusPx, pad + radiusPx) { drawGearLayers(painter, radiusPx, ringPx, drop, tier) }
        }
        GearGlyph(image, radiusPx, pad + radiusPx)
    }
}

private fun DrawScope.drawGearLayers(painter: VectorPainter, radius: Float, ringPx: Float, drop: Float, tier: VisualTier) {
    val path = cookiePath(radius)
    val shadowAlpha = GlassSpecs.watch(tier).shadowAlpha
    if (shadowAlpha > 0f) translate(0f, drop) { drawPath(path, OemColors.Background, shadowAlpha) }
    if (tier == VisualTier.REFINED) {
        drawPath(path, Brush.verticalGradient(listOf(OemColors.AccentLight, OemColors.AccentDeep), -radius, radius))
    } else {
        drawPath(path, OemColors.Accent)
    }
    drawPath(path, OemColors.RimLight, style = Stroke(ringPx))
    val edge = radius * 2f * WearUiTokens.COOKIE_GEAR_FRACTION
    translate(-edge / 2f, -edge / 2f) {
        with(painter) { draw(Size(edge, edge), 1f, ColorFilter.tint(OemColors.OnAccent)) }
    }
}

private fun cookiePath(radius: Float): Path {
    val points = CookieOutline.points(radius)
    return Path().apply {
        moveTo(points[0], points[1])
        var index = 2
        while (index < points.size) {
            lineTo(points[index], points[index + 1])
            index += 2
        }
        close()
    }
}

@Composable
fun CookieGearButton(
    label: String,
    tier: VisualTier,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    longClickLabel: String,
) {
    val radius = with(LocalDensity.current) { (WearUiTokens.COOKIE_BUTTON_DP / 2f).dp.toPx() }
    val glyph = rememberGearGlyph(radius, tier)
    androidx.compose.foundation.Canvas(
        modifier
            .size(WearUiTokens.COOKIE_BUTTON_DP.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = longClickLabel)
            .semantics {
                contentDescription = label
                onClick(label) { onClick(); true }
            },
    ) {
        drawGearCell(glyph, size.width / 2f, size.height / 2f, size.minDimension / 2f, 1f)
    }
}

internal fun DrawScope.drawGearCell(
    glyph: GearGlyph,
    x: Float,
    y: Float,
    radius: Float,
    alpha: Float,
    pressed: Boolean = false,
) {
    val pressedScale = if (pressed) WearUiTokens.COOKIE_PRESS_SCALE else 1f
    translate(x, y) {
        scale(radius / glyph.radius * pressedScale, pivot = Offset.Zero) {
            drawImage(
                glyph.image,
                dstOffset = glyph.dstOffset,
                dstSize = glyph.dstSize,
                alpha = alpha,
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}
