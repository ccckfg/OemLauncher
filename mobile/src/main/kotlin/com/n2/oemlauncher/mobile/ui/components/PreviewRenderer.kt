package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import androidx.core.graphics.withSave
import com.n2.oemlauncher.mobile.config.PreviewTokens
import com.n2.oemlauncher.shared.design.LabelZone
import com.n2.oemlauncher.shared.design.Palettes
import com.n2.oemlauncher.shared.layout.DisplayMath
import com.n2.oemlauncher.shared.layout.FisheyeLens
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.HoneycombProjector
import com.n2.oemlauncher.shared.layout.HoneycombSlots
import com.n2.oemlauncher.shared.layout.PanBounds
import com.n2.oemlauncher.shared.layout.RoundScreen
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.WatchDisplay
import kotlin.math.max

/**
 * Draws the redesigned watch launcher into the phone preview: the refined or lite backdrop, a
 * cookie-shaped settings gear in honeycomb cell 0 (and in the first grid and list row), icons
 * with their rings, and rows that shrink and fade towards the round edges. Every shader, path
 * and paint is built in the constructor, so [draw] allocates nothing.
 */
class PreviewRenderer(
    private val apps: List<CatalogEntry>,
    private val display: WatchDisplay,
    private val settings: LayoutSettings,
    private val images: List<Bitmap?>,
    private val gear: Bitmap? = null,
    private val settingsLabel: String = "",
    private val favoriteCount: Int = 0,
) {
    // Named "watch*" on purpose: plain width/height inside a Canvas receiver lambda would bind
    // to Canvas.getWidth()/getHeight() and silently drop the preview scale.
    private val watchWidth = display.widthPx.toFloat()
    private val watchHeight = display.heightPx.toFloat()
    private val radius = minOf(watchWidth, watchHeight) / 2f
    private val density = display.densityDpi
    private val diameter = DisplayMath.dpToPx(HoneycombDefaults.iconDiameterDp(settings.iconSize), density)
    private val cells = HoneycombSlots.cellCount(apps.size)
    val geometry = HoneycombGeometry(cells, HoneycombDefaults.pitchFor(diameter))
    private val projector = HoneycombProjector(geometry, FisheyeLens(settings.fisheyeStrength), diameter / 2f)
    private val frame = HoneycombFrame(cells)
    val bounds = PanBounds.forGeometry(geometry, radius)
    val scratch = FloatArray(2)
    private val backdrop = WatchBackdrop(watchWidth, watchHeight, settings.visualTier)
    private val painter = PreviewIconPainter(
        density, settings.iconShape, settings.iconStyle, images,
        apps.map { it.label.take(1) }, favoriteCount, gear, settings.visualTier,
    )
    private val gearSize = DisplayMath.dpToPx(PreviewTokens.GEAR_BUTTON_DP, density)
    private val gridTop = DisplayMath.dpToPx(PreviewTokens.GRID_TOP_DP, density)
    private val gridRow = diameter + DisplayMath.dpToPx(PreviewTokens.GRID_LABEL_GAP_DP, density)
    private val gearRow = gearSize + DisplayMath.dpToPx(PreviewTokens.GRID_GEAR_GAP_DP, density)
    private val listRow = max(diameter, DisplayMath.dpToPx(PreviewTokens.LIST_ROW_DP, density))
    private val arcPx = radius * PreviewTokens.LIST_ARC_FACTOR
    private val columns = settings.gridColumns.coerceAtLeast(1)
    private val gridText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Palettes.Mocha.textPrimary.toInt()
        textSize = DisplayMath.dpToPx(PreviewTokens.GRID_LABEL_SP, density)
        textAlign = Paint.Align.CENTER
    }
    private val listText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Palettes.Mocha.textPrimary.toInt()
        textSize = DisplayMath.dpToPx(PreviewTokens.LIST_LABEL_SP, density)
        textAlign = Paint.Align.LEFT
    }
    // Two passes, both built here: the background-colored outline goes down first, the name on
    // top of it. A blur would smear the glyph edges, which is why the outline is a stroke.
    private val focusOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Palettes.Mocha.background.toInt()
        alpha = (LabelZone.OUTLINE_ALPHA * 255f).toInt()
        textSize = DisplayMath.dpToPx(PreviewTokens.FOCUS_LABEL_SP, density)
        textAlign = Paint.Align.LEFT
        style = Paint.Style.STROKE
        strokeWidth = DisplayMath.dpToPx(LabelZone.OUTLINE_WIDTH_DP, density)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val focusText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Palettes.Mocha.textSecondary.toInt()
        textSize = DisplayMath.dpToPx(PreviewTokens.FOCUS_LABEL_SP, density)
        textAlign = Paint.Align.LEFT
    }
    private val labelPath = Path()
    private val pathMeasure = PathMeasure()
    private val labelArcLength: Float
    val scrollLimit = max(0f, contentHeight() - watchHeight + gridTop * 2f)
    private val gridLabels = apps.map {
        it.label.take(gridText.breakText(it.label, true, watchWidth / columns, null))
    }
    private val listLabels = apps.map {
        it.label.take(listText.breakText(it.label, true, watchWidth - diameter - gridTop * 2f, null))
    }

    init {
        // The focus name follows the bottom bezel, like the watch draws it.
        val arcRadius = radius * PreviewTokens.FOCUS_LABEL_RADIUS_FRACTION
        val half = PreviewTokens.FOCUS_LABEL_SWEEP_DEGREES / 2f
        labelPath.addArc(
            watchWidth / 2f - arcRadius, watchHeight / 2f - arcRadius,
            watchWidth / 2f + arcRadius, watchHeight / 2f + arcRadius,
            90f + half, -PreviewTokens.FOCUS_LABEL_SWEEP_DEGREES,
        )
        pathMeasure.setPath(labelPath, false)
        labelArcLength = pathMeasure.length
    }

    private fun contentHeight(): Float = if (settings.mode == LayoutMode.LIST) {
        // List capsules stack like the watch's list, so only the grid keeps the gear gap.
        gearSize + apps.size * listRow
    } else {
        gearRow + RoundScreen.rowCount(apps.size, columns) * gridRow
    }

    fun draw(canvas: Canvas, drawWidth: Float, drawHeight: Float, panX: Float, panY: Float) {
        canvas.withSave {
            val scale = minOf(drawWidth / watchWidth, drawHeight / watchHeight)
            translate((drawWidth - watchWidth * scale) / 2f, (drawHeight - watchHeight * scale) / 2f)
            scale(scale, scale)
            backdrop.drawBackground(this)
            when (settings.mode) {
                LayoutMode.HONEYCOMB -> drawHoneycomb(this, panX, panY)
                LayoutMode.GRID -> drawGrid(this, panY)
                LayoutMode.LIST -> drawList(this, panY)
            }
            // The vignette sinks the icons into the bezel; the focus name stays above it.
            backdrop.drawVignette(this)
            if (settings.mode == LayoutMode.HONEYCOMB) drawFocusLabel(this, panX, panY)
        }
    }

    /** Projects one frame for the honeycomb; the grid and the list scroll linearly instead. */
    private fun prepareFrame(panX: Float, panY: Float) {
        projector.project(panX, panY, radius, frame)
    }

    /**
     * Cells the renderer would draw at [panX]/[panY], four values per cell (x, y, size, alpha)
     * in watch pixels; returns how many entries were written. Reused buffer, no allocations.
     */
    fun visibleIcons(panX: Float, panY: Float, out: FloatArray): Int {
        require(out.size >= frame.size * 4) { "output buffer too small" }
        prepareFrame(panX, panY)
        var count = 0
        for (cell in 0 until frame.size) {
            if (frame.alpha[cell] <= 0f) continue
            out[count * 4] = watchWidth / 2f + frame.x[cell]
            out[count * 4 + 1] = watchHeight / 2f + frame.y[cell]
            out[count * 4 + 2] = diameter * frame.scale[cell]
            out[count * 4 + 3] = frame.alpha[cell]
            count++
        }
        return count
    }

    /** Cell drawn closest to the screen center at [panX]/[panY], or -1 when nothing is visible. */
    fun centerCell(panX: Float, panY: Float): Int {
        prepareFrame(panX, panY)
        var focused = -1
        var nearest = Float.MAX_VALUE
        for (cell in 0 until frame.size) {
            if (frame.alpha[cell] <= 0f) continue
            val distance = frame.x[cell] * frame.x[cell] + frame.y[cell] * frame.y[cell]
            if (distance < nearest) {
                nearest = distance
                focused = cell
            }
        }
        return focused
    }

    private fun drawHoneycomb(canvas: Canvas, x: Float, y: Float) {
        prepareFrame(x, y)
        for (cell in 0 until frame.size) {
            if (frame.alpha[cell] <= 0f) continue
            val cx = watchWidth / 2f + frame.x[cell]
            val cy = watchHeight / 2f + frame.y[cell]
            val size = diameter * frame.scale[cell]
            // Cells that slide into the bottom strip fade, so the curved name stays readable.
            val alpha = if (settings.showLabels) {
                frame.alpha[cell] * LabelZone.alpha(frame.y[cell], size / 2f, radius)
            } else {
                frame.alpha[cell]
            }
            if (alpha <= 0f) continue
            if (HoneycombSlots.isSettings(cell)) {
                painter.gear(canvas, cx, cy, size / 2f, alpha)
            } else {
                painter.icon(canvas, HoneycombSlots.appAt(cell), cx, cy, size, alpha)
            }
        }
    }

    /** Name of the centered cell along the bottom bezel; nothing is drawn when labels are off. */
    private fun drawFocusLabel(canvas: Canvas, panX: Float, panY: Float) {
        if (!settings.showLabels) return
        val cell = centerCell(panX, panY)
        if (cell < 0) return
        val label = if (HoneycombSlots.isSettings(cell)) {
            settingsLabel
        } else {
            apps.getOrNull(HoneycombSlots.appAt(cell))?.label.orEmpty()
        }
        if (label.isEmpty()) return
        val offset = (labelArcLength - focusText.measureText(label)) / 2f
        val start = offset.coerceAtLeast(0f)
        canvas.drawTextOnPath(label, labelPath, start, 0f, focusOutline)
        canvas.drawTextOnPath(label, labelPath, start, 0f, focusText)
    }

    private fun drawGrid(canvas: Canvas, y: Float) {
        val cellWidth = watchWidth / columns
        val gearCy = gridTop + gearSize / 2f + y
        if (gearCy + gearSize > 0f && gearCy - gearSize < watchHeight) {
            val position = gearCy / watchHeight
            val scale = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_SCALE)
            val alpha = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_ALPHA)
            painter.gear(canvas, watchWidth / 2f, gearCy, gearSize / 2f * scale, alpha)
        }
        for (i in apps.indices) {
            val cx = (i % columns + 0.5f) * cellWidth
            val cy = gridTop + gearRow + diameter / 2f + i / columns * gridRow + y
            if (cy + diameter < 0f || cy - diameter > watchHeight) continue
            val position = cy / watchHeight
            val scale = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_SCALE)
            val alpha = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_ALPHA)
            val size = minOf(diameter, cellWidth) * scale
            painter.icon(canvas, i, cx, cy, size, alpha)
            if (settings.showLabels) {
                gridText.alpha = (alpha * 255f).toInt()
                canvas.drawText(gridLabels[i], cx, cy + size / 2f + gridText.textSize, gridText)
            }
        }
        gridText.alpha = 255
    }

    private fun drawList(canvas: Canvas, y: Float) {
        val gearCy = gridTop + gearSize / 2f + y
        if (gearCy + gearSize > 0f && gearCy - gearSize < watchHeight) {
            val position = gearCy / watchHeight
            val scale = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_SCALE)
            val alpha = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_ALPHA)
            painter.gear(canvas, watchWidth / 2f, gearCy, gearSize / 2f * scale, alpha)
        }
        for (i in apps.indices) {
            val cy = gridTop + gearSize + listRow / 2f + i * listRow + y
            if (cy + listRow < 0f || cy - listRow > watchHeight) continue
            val position = cy / watchHeight
            val scale = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_SCALE)
            val alpha = PreviewEdge.falloff(position, PreviewTokens.EDGE_MIN_ALPHA)
            val halfChord = if (display.isRound) {
                RoundScreen.chordHalfWidth(cy - watchHeight / 2f, radius)
            } else {
                radius
            }
            val left = watchWidth / 2f - halfChord + gridTop + PreviewEdge.arcInset(position) * arcPx
            val right = watchWidth / 2f + halfChord - gridTop
            val half = listRow / 2f * scale
            painter.rowCapsule(canvas, left, cy - half, right, cy + half, alpha)
            val size = diameter * scale
            val iconCx = if (settings.showLabels) left + gridTop + size / 2f else watchWidth / 2f
            painter.icon(canvas, i, iconCx, cy, size, alpha)
            if (settings.showLabels) {
                listText.alpha = (alpha * 255f).toInt()
                canvas.drawText(
                    listLabels[i], iconCx + size / 2f + gridTop / 2f,
                    cy - (listText.ascent() + listText.descent()) / 2f, listText,
                )
            }
        }
        listText.alpha = 255
    }

    companion object {
        /** Keep vertical pan inside the current content bounds after layout settings change. */
        fun clampedVerticalPan(panY: Float, scrollLimit: Float): Float =
            panY.coerceIn(-scrollLimit.coerceAtLeast(0f), 0f)
    }
}
