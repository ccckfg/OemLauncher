package com.n2.oemlauncher.wear.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.curvedText
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.shared.design.CookieOutline
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.wear.ui.theme.OemColors
import kotlin.math.roundToInt

/** Gear resources prepared once, so drawing the gear cell allocates nothing per frame. */
class GearGlyph(
    val painter: VectorPainter,
    val tint: ColorFilter,
    val ring: Stroke,
    val cookiePath: Path,
    val cookieFill: Brush?,
    val cookieColor: androidx.compose.ui.graphics.Color,
    val shadow: Boolean,
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
            cookieColor = OemColors.Accent,
            shadow = tier == VisualTier.REFINED,
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
    val scale = if (pressed) WearUiTokens.COOKIE_PRESS_SCALE else 1f
    translate(x, y) {
        if (glyph.shadow) {
            translate(0f, radius * 0.08f) { drawPath(glyph.cookiePath, OemColors.Background.copy(alpha = 0.35f * alpha)) }
        }
        if (glyph.cookieFill != null) drawPath(glyph.cookiePath, glyph.cookieFill, alpha)
        else drawPath(glyph.cookiePath, glyph.cookieColor, alpha)
        drawPath(glyph.cookiePath, OemColors.RimLight, alpha, style = glyph.ring)
        val edge = radius * 2f * WearUiTokens.COOKIE_GEAR_FRACTION * scale
        translate(-edge / 2f, -edge / 2f) {
            with(glyph.painter) { draw(Size(edge, edge), alpha, glyph.tint) }
        }
    }
}

internal fun DrawScope.drawAppCell(
    icon: ImageBitmap?,
    x: Float,
    y: Float,
    radius: Float,
    alpha: Float,
    favorite: Boolean = false,
) {
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
    drawCircle(OemColors.RimLight, radius - WearUiTokens.APP_RIM_DP / 2f, Offset(x, y), alpha, style = Stroke(WearUiTokens.APP_RIM_DP))
    if (favorite) {
        drawCircle(OemColors.Accent, radius - WearUiTokens.FAVORITE_RIM_DP / 2f, Offset(x, y), alpha, style = Stroke(WearUiTokens.FAVORITE_RIM_DP))
    }
}

/** Name of the centered cell, curved along the bottom bezel; it fades out while dragging. */
@Composable
fun HoneycombFocusLabel(text: String, visible: Boolean) {
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(WearUiTokens.FOCUS_LABEL_FADE_MS),
        label = "focusLabel",
    )
    val color = OemColors.TextSecondary
    CurvedLayout(
        // Decorative: every cell already has its own accessibility node with the same name.
        Modifier
            .fillMaxSize()
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = OemColors.Background.copy(alpha = 0.85f),
                spotColor = OemColors.Background.copy(alpha = 0.85f),
            )
            .clearAndSetSemantics {}
            .graphicsLayer { this.alpha = alpha },
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
