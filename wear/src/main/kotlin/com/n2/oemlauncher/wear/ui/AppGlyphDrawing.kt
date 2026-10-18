package com.n2.oemlauncher.wear.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.theme.OemColors

/** All shape geometry is built at the base size and transformed with the cell, not rebuilt. */
internal class AppGlyphDrawing(val shape: IconShape, val diameter: Float, rimPx: Float, favoritePx: Float) {
    val mask = outline(0f)
    val rim = outline(rimPx / 2f)
    val favoriteRing = outline(favoritePx / 2f)
    val rimStroke = Stroke(rimPx)
    val favoriteStroke = Stroke(favoritePx)

    private fun outline(inset: Float) = Path().apply {
        val rect = Rect(inset, inset, diameter - inset, diameter - inset)
        if (shape == IconShape.SQUIRCLE) {
            val corner = (diameter * WearUiTokens.ICON_CORNER_FRACTION - inset).coerceAtLeast(0f)
            addRoundRect(RoundRect(rect, CornerRadius(corner)))
        } else addOval(rect)
    }
}

internal fun DrawScope.drawAppCell(
    glyph: AppGlyphDrawing,
    icon: ImageBitmap?,
    x: Float,
    y: Float,
    radius: Float,
    alpha: Float,
    favorite: Boolean = false,
) {
    translate(x - radius, y - radius) {
        scale(radius * 2f / glyph.diameter, pivot = Offset.Zero) {
            if (glyph.shape == IconShape.ORIGINAL) {
                drawGlyphImage(glyph, icon, alpha)
            } else {
                clipPath(glyph.mask) { drawGlyphImage(glyph, icon, alpha) }
                drawPath(glyph.rim, OemColors.RimLight, alpha, style = glyph.rimStroke)
                if (favorite) drawPath(glyph.favoriteRing, OemColors.Accent, alpha, style = glyph.favoriteStroke)
            }
        }
    }
}

private fun DrawScope.drawGlyphImage(glyph: AppGlyphDrawing, icon: ImageBitmap?, alpha: Float) {
    if (icon == null) {
        drawPath(glyph.mask, OemColors.Placeholder, alpha)
    } else {
        drawImage(
            icon,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(icon.width, icon.height),
            dstSize = IntSize(glyph.diameter.toInt(), glyph.diameter.toInt()),
            alpha = alpha,
            filterQuality = FilterQuality.Medium,
        )
    }
}
