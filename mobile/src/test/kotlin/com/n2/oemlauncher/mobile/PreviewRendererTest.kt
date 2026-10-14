package com.n2.oemlauncher.mobile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.n2.oemlauncher.mobile.ui.components.PreviewRenderer
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.WatchDisplay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreviewRendererTest {
    private val display = WatchDisplay(454, 454, 320, true)
    private val apps = (1..30).map { CatalogEntry("com.example.app$it/com.example.app$it.Main", "App $it") }

    private fun renderer(columns: Int, mode: LayoutMode = LayoutMode.GRID) = PreviewRenderer(
        apps, display, LayoutSettings(mode = mode, gridColumns = columns), List(apps.size) { null },
    )

    @Test fun scrollLimitShrinksWhenGridColumnsIncrease() {
        assertTrue(renderer(2).scrollLimit > renderer(4).scrollLimit)
    }

    @Test fun clampedVerticalPanKeepsOldPanInsideNewBounds() {
        val wide = renderer(2)
        val narrow = renderer(4)
        val pan = -wide.scrollLimit
        assertEquals(-narrow.scrollLimit, PreviewRenderer.clampedVerticalPan(pan, narrow.scrollLimit))
        assertEquals(0f, PreviewRenderer.clampedVerticalPan(50f, narrow.scrollLimit))
        assertEquals(pan, PreviewRenderer.clampedVerticalPan(pan, wide.scrollLimit))
    }

    /** Guards the "watch px vs canvas px" mix-up: at pan zero the preview must stay centered. */
    @Test fun honeycombIsCenteredAndScaledAtPanZero() {
        val icons = listOf(
            Bitmap.createBitmap(104, 104, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) },
            Bitmap.createBitmap(104, 104, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) },
        )
        val bitmap = Bitmap.createBitmap(703, 703, Bitmap.Config.ARGB_8888)
        PreviewRenderer(apps.take(2), display, LayoutSettings(), icons)
            .draw(Canvas(bitmap), 703f, 703f, 0f, 0f)
        val red = bbox(bitmap) { r, g, b -> r > 150 && g < 100 && b < 100 }
        val scale = 703f / 454f
        assertEquals(351.5f, (red[0] + red[1]) / 2f, 6f)
        assertEquals(351.5f, (red[2] + red[3]) / 2f, 6f)
        assertEquals(104f * scale, red[1] - red[0], 6f)
    }

    private fun bbox(bitmap: Bitmap, matches: (Int, Int, Int) -> Boolean): FloatArray {
        var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE; var maxX = -1; var maxY = -1
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val c = bitmap.getPixel(x, y)
                if (matches(Color.red(c), Color.green(c), Color.blue(c))) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
        }
        return floatArrayOf(minX.toFloat(), maxX.toFloat(), minY.toFloat(), maxY.toFloat())
    }
}
