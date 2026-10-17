package com.n2.oemlauncher.mobile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.n2.oemlauncher.mobile.config.PreviewTokens
import com.n2.oemlauncher.mobile.ui.components.PreviewEdge
import com.n2.oemlauncher.mobile.ui.components.PreviewRenderer
import com.n2.oemlauncher.mobile.ui.components.WatchBackdrop
import com.n2.oemlauncher.shared.layout.DisplayMath
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombSlots
import com.n2.oemlauncher.shared.layout.RoundScreen
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.shared.model.WatchDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreviewRendererTest {
    private val display = WatchDisplay(454, 454, 320, true)
    private val apps = (1..30).map { CatalogEntry("com.example.app$it/com.example.app$it.Main", "App $it") }
    private val side = 703
    private val columns = 4

    private fun renderer(
        mode: LayoutMode = LayoutMode.GRID,
        columns: Int = this.columns,
        tier: VisualTier = VisualTier.REFINED,
        icons: List<Bitmap?> = List(apps.size) { null },
    ) = PreviewRenderer(
        apps, display, LayoutSettings(mode = mode, gridColumns = columns, visualTier = tier),
        icons, gear = null, settingsLabel = SETTINGS_LABEL, favoriteCount = 2,
    )

    private fun icon(color: Int) =
        Bitmap.createBitmap(104, 104, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    /** The watch reserves cell 0 for the gear, so the preview must have one cell more. */
    @Test fun honeycombReservesTheFirstCellForTheSettingsGear() {
        val renderer = renderer(mode = LayoutMode.HONEYCOMB)
        assertEquals(apps.size + 1, renderer.geometry.count)
        assertEquals(HoneycombSlots.cellCount(apps.size), renderer.geometry.count)
        assertEquals(HoneycombSlots.SETTINGS, renderer.centerCell(0f, 0f))
    }

    /** At rest the center of the screen is the champagne cookie, not an app icon. */
    @Test fun honeycombDrawsTheGearCookieInTheCenter() {
        val icons = listOf(icon(Color.RED), icon(Color.BLUE)) + List(apps.size - 2) { icon(Color.GREEN) }
        val renderer = PreviewRenderer(
            apps, display, LayoutSettings(), icons,
            gear = null, settingsLabel = SETTINGS_LABEL, favoriteCount = 0,
        )
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        renderer.draw(Canvas(bitmap), side.toFloat(), side.toFloat(), 0f, 0f)
        val scale = side / 454f
        val center = side / 2f
        // Between the glyph (half the diameter) and the cookie edge: cookie fill only.
        val radius = 104f * scale * 0.4f
        var champagne = 0
        for (i in 0 until SAMPLES) {
            val angle = i * 2.0 * PI / SAMPLES
            val x = (center + radius * cos(angle)).toInt()
            val y = (center + radius * sin(angle)).toInt()
            val color = bitmap.getPixel(x, y)
            if (Color.red(color) > 120 && Color.red(color) > Color.blue(color)) champagne++
        }
        assertTrue("cookie fill expected around the center, found $champagne of $SAMPLES", champagne >= 6)
    }

    /** The gear row is content: the scroll limit must be exactly one gear row taller. */
    @Test fun gridAndListScrollLimitsIncludeTheGearRow() {
        val density = display.densityDpi
        val gear = DisplayMath.dpToPx(PreviewTokens.GEAR_BUTTON_DP, density)
        val top = DisplayMath.dpToPx(PreviewTokens.GRID_TOP_DP, density)
        val gridRow = DisplayMath.dpToPx(HoneycombDefaults.iconDiameterDp(LayoutSettings().iconSize), density) +
            DisplayMath.dpToPx(PreviewTokens.GRID_LABEL_GAP_DP, density)
        val listRow = maxOf(
            DisplayMath.dpToPx(HoneycombDefaults.iconDiameterDp(LayoutSettings().iconSize), density),
            DisplayMath.dpToPx(PreviewTokens.LIST_ROW_DP, density),
        )
        val height = display.heightPx.toFloat()
        val gridWithoutGear = RoundScreen.rowCount(apps.size, columns) * gridRow + top * 2f - height
        val listWithoutGear = apps.size * listRow + top * 2f - height
        assertEquals(gear, renderer(mode = LayoutMode.GRID).scrollLimit - gridWithoutGear, 0.5f)
        assertEquals(gear, renderer(mode = LayoutMode.LIST).scrollLimit - listWithoutGear, 0.5f)
        assertTrue(renderer(mode = LayoutMode.GRID).scrollLimit > 0f)
        assertTrue(renderer(mode = LayoutMode.LIST).scrollLimit > 0f)
    }

    @Test fun scrollLimitShrinksWhenGridColumnsIncrease() {
        assertTrue(renderer(columns = 2).scrollLimit > renderer(columns = 4).scrollLimit)
    }

    @Test fun clampedVerticalPanKeepsOldPanInsideNewBounds() {
        val wide = renderer(columns = 2)
        val narrow = renderer(columns = 4)
        val pan = -wide.scrollLimit
        assertEquals(-narrow.scrollLimit, PreviewRenderer.clampedVerticalPan(pan, narrow.scrollLimit))
        assertEquals(0f, PreviewRenderer.clampedVerticalPan(50f, narrow.scrollLimit))
        assertEquals(pan, PreviewRenderer.clampedVerticalPan(pan, wide.scrollLimit))
    }

    /** Ported from wear's RoundEdge: full size in the middle band, the minimum at the edges. */
    @Test fun edgeFalloffKeepsTheMiddleFlatAndShrinksAtTheEdges() {
        val min = PreviewTokens.EDGE_MIN_SCALE
        assertEquals(1f, PreviewEdge.falloff(0.5f, min), EPSILON)
        assertEquals(min, PreviewEdge.falloff(0f, min), EPSILON)
        assertEquals(min, PreviewEdge.falloff(1f, min), EPSILON)
        val band = PreviewTokens.EDGE_FLAT_FRACTION / 2f
        assertEquals(1f, PreviewEdge.falloff(0.5f - band, min), EPSILON)
        assertEquals(1f, PreviewEdge.falloff(0.5f + band, min), EPSILON)
        assertTrue(PreviewEdge.falloff(0.2f, min) > min)
        assertTrue(PreviewEdge.falloff(0.2f, min) < 1f)
    }

    @Test fun arcInsetFollowsTheCircleEdge() {
        assertEquals(0f, PreviewEdge.arcInset(0.5f), EPSILON)
        assertEquals(1f, PreviewEdge.arcInset(0f), EPSILON)
        assertEquals(1f, PreviewEdge.arcInset(1f), EPSILON)
        val quarter = PreviewEdge.arcInset(0.25f)
        assertTrue(quarter > 0f && quarter < 0.4f)
    }

    /** The lite tier draws a flat background, so no glow shader may be built for it. */
    @Test fun liteTierBuildsNoGlowShaders() {
        val lite = WatchBackdrop(454f, 454f, VisualTier.LITE)
        assertEquals(0, lite.glowShaderCount)
        assertFalse(lite.ambientGlow)
        val refined = WatchBackdrop(454f, 454f, VisualTier.REFINED)
        assertEquals(2, refined.glowShaderCount)
        assertTrue(refined.ambientGlow)
    }

    /** Every drawn cell is reported, gear included, in watch pixels. */
    @Test fun visibleIconsReportsCellsWithTheirScreenPositions() {
        val renderer = renderer(mode = LayoutMode.HONEYCOMB)
        val quads = FloatArray((apps.size + 1) * 4)
        val count = renderer.visibleIcons(0f, 0f, quads)
        assertTrue(count > 0)
        var gear = false
        for (i in 0 until count) {
            val x = quads[i * 4]
            val y = quads[i * 4 + 1]
            val size = quads[i * 4 + 2]
            val alpha = quads[i * 4 + 3]
            assertTrue(size > 0f)
            assertTrue(alpha > 0f)
            if (x == 227f && y == 227f) gear = true
        }
        assertTrue("the gear sits at the center at rest", gear)
    }

    private companion object {
        const val SETTINGS_LABEL = "Settings"
        const val SAMPLES = 8
        const val EPSILON = 1e-4f
    }
}
