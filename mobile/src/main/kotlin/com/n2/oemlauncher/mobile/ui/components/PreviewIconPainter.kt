package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.withSave
import com.n2.oemlauncher.mobile.config.PreviewTokens
import com.n2.oemlauncher.shared.design.CookieOutline
import com.n2.oemlauncher.shared.design.Palettes
import com.n2.oemlauncher.shared.layout.DisplayMath
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.VisualTier

/**
 * Paints the watch's icons, rings and the centered settings gear onto the preview canvas. The
 * watch always runs the Mocha palette, so the phone shows it in Mocha even in the light theme.
 * Every paint, shader and path is built here once; drawing allocates nothing.
 */
class PreviewIconPainter(
    densityDpi: Int,
    private val iconShape: IconShape,
    private val images: List<Bitmap?>,
    private val initials: List<String>,
    private val favoriteCount: Int,
    private val gear: Bitmap?,
    tier: VisualTier,
) {
    private val palette = Palettes.Mocha
    private val rimPx = DisplayMath.dpToPx(PreviewTokens.ICON_RIM_DP, densityDpi)
    private val favoriteRingPx = DisplayMath.dpToPx(PreviewTokens.FAVORITE_RING_DP, densityDpi)
    private val favoriteGapPx = DisplayMath.dpToPx(PreviewTokens.FAVORITE_RING_GAP_DP, densityDpi)
    private val gearRimPx = DisplayMath.dpToPx(PreviewTokens.GEAR_RIM_DP, densityDpi)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.textPrimary.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val cookiePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (tier == VisualTier.REFINED) {
            shader = LinearGradient(
                0f, -1f, 0f, 1f,
                palette.accentLight.toInt(), palette.accentDeep.toInt(), Shader.TileMode.CLAMP,
            )
            setShadowLayer(
                DisplayMath.dpToPx(PreviewTokens.GEAR_SHADOW_DP, densityDpi),
                0f, DisplayMath.dpToPx(PreviewTokens.GEAR_SHADOW_DP, densityDpi) / 2f,
                Color.argb((PreviewTokens.GEAR_SHADOW_ALPHA * 255f).toInt(), 0, 0, 0),
            )
        } else {
            color = palette.accent.toInt()
        }
    }
    private val iconPath = Path()
    private val ringPath = Path()
    private val cookiePath = unitCookie()
    private val rect = RectF()

    /** One app icon with its hairline rim, plus the champagne ring for favorites. */
    fun icon(canvas: Canvas, index: Int, x: Float, y: Float, size: Float, alpha: Float) {
        val half = size / 2f
        val bitmap = images.getOrNull(index)
        outline(iconPath, x, y, half, 0f)
        bitmapPaint.alpha = alphaOf(alpha)
        canvas.withSave {
            clipPath(iconPath)
            if (bitmap != null) {
                rect.set(x - half, y - half, x + half, y + half)
                drawBitmap(bitmap, null, rect, bitmapPaint)
            } else {
                fillPaint.color = Color.DKGRAY
                fillPaint.alpha = alphaOf(alpha)
                drawCircle(x, y, half, fillPaint)
                initialPaint.textSize = size * INITIAL_TEXT_RATIO
                initialPaint.alpha = alphaOf(alpha)
                drawText(initials.getOrNull(index).orEmpty(), x, y - (initialPaint.ascent() + initialPaint.descent()) / 2f, initialPaint)
            }
        }
        bitmapPaint.alpha = FULL_ALPHA
        stroke(canvas, iconPath, palette.rimLight, rimPx, alpha)
        if (index in 0 until favoriteCount) {
            outline(ringPath, x, y, half, favoriteGapPx + favoriteRingPx / 2f)
            stroke(canvas, ringPath, palette.accent, favoriteRingPx, alpha)
        }
    }

    /** The cookie-shaped settings gear: champagne fill, rim, then the glyph at half its width. */
    fun gear(canvas: Canvas, x: Float, y: Float, radius: Float, alpha: Float) {
        if (radius <= 0f) return
        cookiePaint.alpha = alphaOf(alpha)
        canvas.withSave {
            translate(x, y)
            scale(radius, radius)
            drawPath(cookiePath, cookiePaint)
            ringPaint.strokeWidth = gearRimPx / radius
            ringPaint.color = palette.rimLight.toInt()
            ringPaint.alpha = alphaOf(alpha)
            drawPath(cookiePath, ringPaint)
        }
        cookiePaint.alpha = FULL_ALPHA
        val glyph = radius * 2f * PreviewTokens.GEAR_GLYPH_FRACTION
        if (gear != null && glyph > 0f) {
            rect.set(x - glyph / 2f, y - glyph / 2f, x + glyph / 2f, y + glyph / 2f)
            bitmapPaint.alpha = alphaOf(alpha)
            canvas.drawBitmap(gear, null, rect, bitmapPaint)
            bitmapPaint.alpha = FULL_ALPHA
        }
    }

    /** Glass capsule behind a list row, with the same rim light the phone's rows use. */
    fun rowCapsule(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, alpha: Float) {
        val corner = (bottom - top) / 2f
        fillPaint.color = palette.glass.toInt()
        fillPaint.alpha = alphaOf(alpha)
        canvas.drawRoundRect(left, top, right, bottom, corner, corner, fillPaint)
        ringPaint.strokeWidth = rimPx
        ringPaint.color = palette.rimLight.toInt()
        ringPaint.alpha = alphaOf(alpha)
        canvas.drawRoundRect(
            left + rimPx / 2f, top + rimPx / 2f, right - rimPx / 2f, bottom - rimPx / 2f,
            corner, corner, ringPaint,
        )
    }

    private fun stroke(canvas: Canvas, path: Path, color: Long, width: Float, alpha: Float) {
        ringPaint.strokeWidth = width
        ringPaint.color = color.toInt()
        ringPaint.alpha = alphaOf(alpha)
        canvas.drawPath(path, ringPaint)
    }

    private fun outline(path: Path, x: Float, y: Float, half: Float, offset: Float) {
        val r = half + offset
        path.rewind()
        when (iconShape) {
            IconShape.CIRCLE -> path.addCircle(x, y, r, Path.Direction.CW)
            IconShape.SQUIRCLE -> {
                rect.set(x - r, y - r, x + r, y + r)
                val corner = r * 2f * PreviewTokens.ICON_CORNER_RATIO
                path.addRoundRect(rect, corner, corner, Path.Direction.CW)
            }
            IconShape.ORIGINAL -> {
                rect.set(x - r, y - r, x + r, y + r)
                path.addRect(rect, Path.Direction.CW)
            }
        }
    }

    private fun alphaOf(alpha: Float): Int = (alpha.coerceIn(0f, 1f) * 255f).toInt()

    /** The cookie outline at unit radius, so one path serves every gear size. */
    private fun unitCookie(): Path {
        val points = CookieOutline.points(1f)
        return Path().apply {
            moveTo(points[0], points[1])
            for (i in 1 until points.size / 2) lineTo(points[i * 2], points[i * 2 + 1])
            close()
        }
    }

    private companion object {
        const val FULL_ALPHA = 255
        const val INITIAL_TEXT_RATIO = 0.42f
    }
}
