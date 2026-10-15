package com.n2.oemlauncher.mobile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.n2.oemlauncher.mobile.config.UiDimensions
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
    private val side = 703

    private fun renderer(columns: Int, mode: LayoutMode = LayoutMode.GRID) = PreviewRenderer(
        apps, display, LayoutSettings(mode = mode, gridColumns = columns), List(apps.size) { null },
    )

    private fun icon(color: Int) =
        Bitmap.createBitmap(104, 104, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

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
        val icons = listOf(icon(Color.RED), icon(Color.BLUE))
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        PreviewRenderer(apps.take(2), display, LayoutSettings(), icons)
            .draw(Canvas(bitmap), side.toFloat(), side.toFloat(), 0f, 0f)
        val red = bbox(bitmap) { r, g, b -> r > 150 && g < 100 && b < 100 }
        val scale = side / 454f
        assertEquals(351.5f, (red[0] + red[1]) / 2f, 6f)
        assertEquals(351.5f, (red[2] + red[3]) / 2f, 6f)
        assertEquals(104f * scale, red[1] - red[0], 6f)
    }

    /** The focus label band hides every icon that would reach it, at rest and panned to the bounds. */
    @Test fun labelBandHidesOverlappingIconsAtEveryPan() {
        val icons = List(apps.size) { icon(Color.RED) }
        val renderer = PreviewRenderer(apps, display, LayoutSettings(), icons)
        val quads = FloatArray(apps.size * 4)
        val limit = renderer.bounds.limitPx
        val pans = listOf(
            0f to 0f, limit to 0f, -limit to 0f, 0f to limit, 0f to -limit,
            limit * 0.7f to -limit * 0.7f, -limit * 0.7f to limit * 0.7f,
        )
        for ((panX, panY) in pans) {
            val count = renderer.visibleIcons(panX, panY, quads)
            for (i in 0 until count) {
                val alpha = quads[i * 4 + 3]
                if (alpha <= UiDimensions.LABEL_BAND_ALPHA_THRESHOLD) continue
                val bottom = quads[i * 4 + 1] + quads[i * 4 + 2] / 2f
                assertTrue(
                    "icon $i reaches the label band at pan ($panX,$panY)",
                    bottom <= renderer.labelBandTop,
                )
            }
        }
        val withLabels = PreviewRenderer(apps, display, LayoutSettings(showLabels = true), icons)
        val withoutLabels = PreviewRenderer(apps, display, LayoutSettings(showLabels = false), icons)
        val kept = withLabels.visibleIcons(0f, 0f, quads)
        val all = withoutLabels.visibleIcons(0f, 0f, quads)
        assertTrue("the label band must clear at least one icon at pan zero", all > kept)
    }

    /** Pixels under the label belong to the scrim and the text, never to an icon. */
    @Test fun labelBandPixelsContainTextButNoIconColors() {
        val renderer = PreviewRenderer(apps, display, LayoutSettings(), List(apps.size) { icon(Color.RED) })
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        renderer.draw(Canvas(bitmap), side.toFloat(), side.toFloat(), 0f, 0f)
        val bandTop = (renderer.labelBandTop * side / 454f).toInt()
        var colored: String? = null
        var textPixels = 0
        for (y in bandTop until side) {
            for (x in 0 until side) {
                val c = bitmap.getPixel(x, y)
                val max = maxOf(Color.red(c), Color.green(c), Color.blue(c))
                val min = minOf(Color.red(c), Color.green(c), Color.blue(c))
                if (max - min > 40 && max > 80) colored = "($x,$y)"
                if (min > 200) textPixels++
            }
        }
        assertNull("icon pixels inside the label band: $colored", colored)
        assertTrue("the focus label must stay visible inside the band", textPixels > 20)
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
