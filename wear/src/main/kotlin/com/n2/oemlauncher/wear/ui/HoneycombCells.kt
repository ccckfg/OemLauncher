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
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.shared.design.CookieOutline
import com.n2.oemlauncher.shared.design.GlassSpecs
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.theme.OemColors

/** Gear resources prepared once, so drawing the gear cell allocates nothing per frame. */
class GearGlyph(
    val painter: VectorPainter,
    val tint: ColorFilter,
    val ring: Stroke,
    val cookiePath: Path,
    val cookieFill: Brush?,
    val radius: Float,
    val shadowAlpha: Float,
)

@Composable
fun rememberGearGlyph(radiusPx: Float, tier: VisualTier): GearGlyph {
    val painter = rememberVectorPainter(Icons.Rounded.Settings)
    val ringPx = with(LocalDensity.current) { WearUiTokens.GEAR_RING_DP.dp.toPx() }
    return remember(painter, ringPx, radiusPx, tier) {
        val points = CookieOutline.points(radiusPx)
        val path = Path().apply {
            moveTo(points[0], points[1])
            var index = 2
            while (index < points.size) {
                lineTo(points[index], points[index + 1])
                index += 2
            }
            close()
        }
        GearGlyph(
            painter = painter,
            tint = ColorFilter.tint(OemColors.OnAccent),
            ring = Stroke(ringPx),
            cookiePath = path,
            cookieFill = if (tier == VisualTier.REFINED) {
                Brush.verticalGradient(listOf(OemColors.AccentLight, OemColors.AccentDeep), -radiusPx, radiusPx)
            } else null,
            radius = radiusPx,
            shadowAlpha = GlassSpecs.watch(tier).shadowAlpha,
        )
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
            if (glyph.shadowAlpha > 0f) {
                translate(0f, glyph.radius * WearUiTokens.COOKIE_SHADOW_OFFSET_FRACTION) {
                    drawPath(glyph.cookiePath, OemColors.Background, glyph.shadowAlpha * alpha)
                }
            }
            if (glyph.cookieFill != null) drawPath(glyph.cookiePath, glyph.cookieFill, alpha)
            else drawPath(glyph.cookiePath, OemColors.Accent, alpha)
            drawPath(glyph.cookiePath, OemColors.RimLight, alpha, style = glyph.ring)
            val edge = glyph.radius * 2f * WearUiTokens.COOKIE_GEAR_FRACTION
            translate(-edge / 2f, -edge / 2f) {
                with(glyph.painter) { draw(Size(edge, edge), alpha, glyph.tint) }
            }
        }
    }
}
