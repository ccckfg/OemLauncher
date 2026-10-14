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
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.shared.layout.DisplayMath
import com.n2.oemlauncher.shared.layout.FisheyeLens
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.HoneycombProjector
import com.n2.oemlauncher.shared.layout.PanBounds
import com.n2.oemlauncher.shared.layout.RoundScreen
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.WatchDisplay
import kotlin.math.max

class PreviewRenderer(
    private val apps: List<CatalogEntry>,
    private val display: WatchDisplay,
    private val settings: LayoutSettings,
    private val images: List<Bitmap?>,
) {
    // Named "watch*" on purpose: plain width/height inside a Canvas receiver lambda would bind
    // to Canvas.getWidth()/getHeight() and silently drop the preview scale.
    private val watchWidth = display.widthPx.toFloat()
    private val watchHeight = display.heightPx.toFloat()
    private val radius = minOf(watchWidth, watchHeight) / 2f
    private val diameter = DisplayMath.dpToPx(
        HoneycombDefaults.iconDiameterDp(settings.iconSize), display.densityDpi,
    )
    val geometry = HoneycombGeometry(apps.size, HoneycombDefaults.pitchFor(diameter))
    private val projector = HoneycombProjector(geometry, FisheyeLens(settings.fisheyeStrength), diameter / 2f)
    private val frame = HoneycombFrame(apps.size)
    val bounds = PanBounds.forGeometry(geometry, radius)
    val scratch = FloatArray(2)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = DisplayMath.dpToPx(UiDimensions.PREVIEW_LABEL_DP, display.densityDpi)
    }
    // The focused label sits on a gradient scrim, like the watch, so icons never touch glyphs.
    private val labelBand = text.textSize * UiDimensions.LABEL_BAND_LINES
    private val scrim = Paint().apply {
        shader = LinearGradient(
            0f, watchHeight - labelBand, 0f, watchHeight,
            Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP,
        )
    }
    private val rect = RectF()
    private val path = Path()
    private val initials = apps.map { it.label.take(1) }
    private val listRow = max(diameter, DisplayMath.dpToPx(UiDimensions.LIST_ROW_DP, display.densityDpi))
    private val gridTop = DisplayMath.dpToPx(UiDimensions.GRID_TOP_DP, display.densityDpi)
    private val gridRow = diameter + DisplayMath.dpToPx(UiDimensions.GRID_LABEL_DP, display.densityDpi)
    val scrollLimit = max(
        0f,
        if (settings.mode == LayoutMode.LIST) apps.size * listRow - watchHeight + gridTop * 2
        else RoundScreen.rowCount(apps.size, settings.gridColumns) * gridRow - watchHeight + gridTop * 2,
    )
    private val gridLabels = apps.map {
        it.label.take(text.breakText(it.label, true, watchWidth / settings.gridColumns, null))
    }
    private val listLabels = apps.map {
        it.label.take(text.breakText(it.label, true, watchWidth - diameter - gridTop * 2, null))
    }

    fun draw(canvas: Canvas, drawWidth: Float, drawHeight: Float, panX: Float, panY: Float) {
        canvas.withSave {
            val scale = minOf(drawWidth / watchWidth, drawHeight / watchHeight)
            translate((drawWidth - watchWidth * scale) / 2f, (drawHeight - watchHeight * scale) / 2f)
            scale(scale, scale)
            drawColor(Color.BLACK)
            when (settings.mode) {
                LayoutMode.HONEYCOMB -> drawHoneycomb(this, panX, panY)
                LayoutMode.GRID -> drawGrid(this, panY)
                LayoutMode.LIST -> drawList(this, panY)
            }
        }
    }

    private fun drawHoneycomb(canvas: Canvas, x: Float, y: Float) {
        projector.project(x, y, radius, frame)
        var focused = -1
        var nearest = Float.MAX_VALUE
        for (i in 0 until frame.size) {
            if (frame.alpha[i] <= 0f) continue
            icon(
                canvas, i, watchWidth / 2f + frame.x[i], watchHeight / 2f + frame.y[i],
                diameter * frame.scale[i], frame.alpha[i],
            )
            val distance = frame.x[i] * frame.x[i] + frame.y[i] * frame.y[i]
            if (distance < nearest) { nearest = distance; focused = i }
        }
        if (focused >= 0 && settings.showLabels) {
            canvas.drawRect(0f, watchHeight - labelBand, watchWidth, watchHeight, scrim)
            text.textAlign = Paint.Align.CENTER
            canvas.drawText(
                apps[focused].label, watchWidth / 2f,
                watchHeight - watchHeight * UiDimensions.LABEL_INSET_RATIO, text,
            )
        }
    }

    private fun drawGrid(canvas: Canvas, y: Float) {
        val columns = settings.gridColumns
        val cellWidth = watchWidth / columns
        for (i in apps.indices) {
            val cx = (i % columns + 0.5f) * cellWidth
            val cy = gridTop + diameter / 2f + i / columns * gridRow + y
            if (cy + diameter < 0 || cy - diameter > watchHeight) continue
            icon(canvas, i, cx, cy, minOf(diameter, cellWidth), 1f)
            if (settings.showLabels) {
                text.textAlign = Paint.Align.CENTER
                canvas.drawText(gridLabels[i], cx, cy + diameter / 2f + text.textSize, text)
            }
        }
    }

    private fun drawList(canvas: Canvas, y: Float) {
        for (i in apps.indices) {
            val cy = gridTop + listRow / 2f + i * listRow + y
            if (cy + listRow < 0 || cy - listRow > watchHeight) continue
            val halfChord = if (display.isRound) {
                RoundScreen.chordHalfWidth(cy - watchHeight / 2f, radius)
            } else radius
            val left = watchWidth / 2f - halfChord + gridTop
            val cx = if (settings.showLabels) left + diameter / 2f else watchWidth / 2f
            icon(canvas, i, cx, cy, diameter, 1f)
            if (settings.showLabels) {
                text.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    listLabels[i], left + diameter + gridTop / 2f,
                    cy - (text.ascent() + text.descent()) / 2f, text,
                )
            }
        }
    }

    private fun icon(canvas: Canvas, index: Int, x: Float, y: Float, size: Float, alpha: Float) {
        rect.set(x - size / 2f, y - size / 2f, x + size / 2f, y + size / 2f)
        path.rewind()
        when (settings.iconShape) {
            IconShape.CIRCLE -> path.addCircle(x, y, size / 2f, Path.Direction.CW)
            IconShape.SQUIRCLE -> path.addRoundRect(
                rect, size * UiDimensions.ICON_CORNER_RATIO, size * UiDimensions.ICON_CORNER_RATIO,
                Path.Direction.CW,
            )
            IconShape.ORIGINAL -> path.addRect(rect, Path.Direction.CW)
        }
        canvas.withSave {
            clipPath(path)
            paint.alpha = (alpha * 255).toInt()
            val bitmap = images.getOrNull(index)
            if (bitmap != null) drawBitmap(bitmap, null, rect, paint) else {
                paint.color = Color.DKGRAY
                paint.alpha = (alpha * 255).toInt()
                drawRect(rect, paint)
                text.textAlign = Paint.Align.CENTER
                drawText(initials[index], x, y - (text.ascent() + text.descent()) / 2f, text)
            }
        }
        paint.alpha = 255
    }

    companion object {
        /** Keep vertical pan inside the current content bounds after layout settings change. */
        fun clampedVerticalPan(panY: Float, scrollLimit: Float): Float =
            panY.coerceIn(-scrollLimit.coerceAtLeast(0f), 0f)
    }
}
