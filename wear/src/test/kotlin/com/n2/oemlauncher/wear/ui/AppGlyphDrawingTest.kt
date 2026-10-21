package com.n2.oemlauncher.wear.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Region
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.n2.oemlauncher.shared.model.IconShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class AppGlyphDrawingTest {
    private fun render(shape: IconShape, favorite: Boolean): Bitmap {
        val target = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }.asImageBitmap()
        val glyph = AppGlyphDrawing(shape, 100f, 2f, 3f)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(target.asImageBitmap()), Size(100f, 100f)) {
            drawAppCell(glyph, image, 50f, 50f, 50f, 1f, favorite)
        }
        return target
    }

    private fun maskContains(shape: IconShape, x: Int, y: Int): Boolean {
        val mask = AppGlyphDrawing(shape, 100f, 2f, 3f).mask.asAndroidPath()
        return Region().apply { setPath(mask, Region(0, 0, 100, 100)) }.contains(x, y)
    }

    @Test fun roundedSquareMaskKeepsTheCornerACircleCuts() {
        assertTrue(maskContains(IconShape.SQUIRCLE, 5, 20))
        assertFalse(maskContains(IconShape.CIRCLE, 5, 20))
    }

    @Test fun drawingDoesNotClipBitmapsThatAreAlreadyMasked() {
        assertEquals(Color.RED, render(IconShape.CIRCLE, false).getPixel(5, 20))
    }

    @Test fun originalIsUnclippedAndHasNoRingsEvenWhenFavorite() {
        val original = render(IconShape.ORIGINAL, true)
        assertEquals(Color.RED, original.getPixel(0, 0))
        assertEquals(Color.RED, original.getPixel(50, 0))
    }

    @Test fun geometryAndStrokesAreStableCachedInstances() {
        val glyph = AppGlyphDrawing(IconShape.SQUIRCLE, 100f, 2f, 3f)
        assertSame(glyph.mask, glyph.mask)
        assertSame(glyph.rim, glyph.rim)
        assertSame(glyph.favoriteRing, glyph.favoriteRing)
        assertSame(glyph.rimStroke, glyph.rimStroke)
        assertSame(glyph.favoriteStroke, glyph.favoriteStroke)
    }
}
