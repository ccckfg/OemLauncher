package com.n2.oemlauncher.wear.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.n2.oemlauncher.shared.design.LabelZone
import com.n2.oemlauncher.shared.design.Palettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FocusLabelDrawingTest {
    @Test fun outlineUsesSharedWidthOpacityAndRoundJoins() {
        val drawing = FocusLabelDrawing("设置", 454f, 454f, 26f, LabelZone.OUTLINE_WIDTH_DP * 2)
        assertEquals(Paint.Style.STROKE, drawing.outline.style)
        assertEquals(Paint.Join.ROUND, drawing.outline.strokeJoin)
        assertEquals(6f, drawing.outline.strokeWidth, 0f)
        assertEquals((255 * LabelZone.OUTLINE_ALPHA).toInt(), drawing.outline.alpha)
        assertEquals(Palettes.Mocha.background.toInt() and 0xffffff, drawing.outline.color and 0xffffff)
        assertEquals(Palettes.Mocha.textSecondary.toInt(), drawing.fill.color)
    }

    @Test fun longNameEllipsizesAndDrawingReusesPathAndPaints() {
        val text = "Very long application name ".repeat(10)
        val drawing = FocusLabelDrawing(text, 454f, 454f, 26f, 6f)
        assertTrue(drawing.label.length < text.length)
        assertTrue(drawing.label.endsWith("…"))
        val path = drawing.path
        val outline = drawing.outline
        val fill = drawing.fill
        val canvas = Canvas(Bitmap.createBitmap(454, 454, Bitmap.Config.ARGB_8888))
        repeat(3) { drawing.draw(canvas) }
        assertSame(path, drawing.path)
        assertSame(outline, drawing.outline)
        assertSame(fill, drawing.fill)
    }
}
