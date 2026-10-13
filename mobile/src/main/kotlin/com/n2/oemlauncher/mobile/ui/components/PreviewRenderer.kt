package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
    private val width = display.widthPx.toFloat()
    private val height = display.heightPx.toFloat()
    private val radius = minOf(width, height) / 2f
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
    private val rect = RectF()
    private val path = Path()
    private val initials = apps.map { it.label.take(1) }
    private val listRow = max(diameter, DisplayMath.dpToPx(UiDimensions.LIST_ROW_DP, display.densityDpi))
    private val gridTop = DisplayMath.dpToPx(UiDimensions.GRID_TOP_DP, display.densityDpi)
    private val gridRow = diameter + DisplayMath.dpToPx(UiDimensions.GRID_LABEL_DP, display.densityDpi)
    val scrollLimit = max(
        0f,
        if (settings.mode == LayoutMode.LIST) apps.size * listRow - height + gridTop * 2
        else RoundScreen.rowCount(apps.size, settings.gridColumns) * gridRow - height + gridTop * 2,
    )
    private val gridLabels = apps.map {
        it.label.take(text.breakText(it.label, true, width / settings.gridColumns, null))
    }
    private val listLabels = apps.map {
        it.label.take(text.breakText(it.label, true, width - diameter - gridTop * 2, null))
    }

    fun draw(canvas: Canvas, drawWidth: Float, drawHeight: Float, panX: Float, panY: Float) {
        canvas.save()
        val scale = minOf(drawWidth / width, drawHeight / height)
        canvas.translate((drawWidth - width * scale) / 2f, (drawHeight - height * scale) / 2f)
        canvas.scale(scale, scale)
        canvas.drawColor(Color.BLACK)
        when (settings.mode) {
            LayoutMode.HONEYCOMB -> drawHoneycomb(canvas, panX, panY)
            LayoutMode.GRID -> drawGrid(canvas, panY)
            LayoutMode.LIST -> drawList(canvas, panY)
        }
        canvas.restore()
    }

    private fun drawHoneycomb(canvas: Canvas, x: Float, y: Float) {
        projector.project(x, y, radius, frame)
        var focused = -1
        var nearest = Float.MAX_VALUE
        for (i in 0 until frame.size) {
            if (frame.alpha[i] <= 0f) continue
            icon(
                canvas, i, width / 2f + frame.x[i], height / 2f + frame.y[i],
                diameter * frame.scale[i], frame.alpha[i],
            )
            val distance = frame.x[i] * frame.x[i] + frame.y[i] * frame.y[i]
            if (distance < nearest) { nearest = distance; focused = i }
        }
        if (focused >= 0 && settings.showLabels) {
            text.textAlign = Paint.Align.CENTER
            canvas.drawText(
                apps[focused].label, width / 2f,
                height - height * UiDimensions.LABEL_INSET_RATIO, text,
            )
        }
    }

    private fun drawGrid(canvas: Canvas, y: Float) {
        val columns = settings.gridColumns
        val cellWidth = width / columns
        for (i in apps.indices) {
            val cx = (i % columns + 0.5f) * cellWidth
            val cy = gridTop + diameter / 2f + i / columns * gridRow + y
            if (cy + diameter < 0 || cy - diameter > height) continue
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
            if (cy + listRow < 0 || cy - listRow > height) continue
            val halfChord = if (display.isRound) RoundScreen.chordHalfWidth(cy - height / 2f, radius) else radius
            val left = width / 2f - halfChord + gridTop
            val cx = if (settings.showLabels) left + diameter / 2f else width / 2f
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
        canvas.save()
        canvas.clipPath(path)
        paint.alpha = (alpha * 255).toInt()
        val bitmap = images.getOrNull(index)
        if (bitmap != null) canvas.drawBitmap(bitmap, null, rect, paint) else {
            paint.color = Color.DKGRAY
            paint.alpha = (alpha * 255).toInt()
            canvas.drawRect(rect, paint)
            text.textAlign = Paint.Align.CENTER
            canvas.drawText(initials[index], x, y - (text.ascent() + text.descent()) / 2f, text)
        }
        canvas.restore()
        paint.alpha = 255
    }
}
