package com.n2.oemlauncher.wear.catalog

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.n2.oemlauncher.shared.design.MochaGlyph
import com.n2.oemlauncher.shared.design.MochaIcons
import com.n2.oemlauncher.shared.model.IconShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MochaIconRendererTest {
    private val icon = MochaIcons.Icon(MochaGlyph.CALL, MochaIcons.Tone.SAGE)

    @Test fun packIconsAreToneDiscsWithCreamInk() {
        val bitmap = MochaIconRenderer.render(icon, 96, IconShape.CIRCLE)
        assertEquals(0, Color.alpha(bitmap.getPixel(1, 1)))
        val tone = MochaIcons.Tone.SAGE.argb.toInt()
        val bottom = bitmap.getPixel(48, 92)
        assertEquals(Color.red(tone), Color.red(bottom), 2)
        assertEquals(Color.green(tone), Color.green(bottom), 2)
        val ink = (0 until 96 * 96).count { bitmap.getPixel(it % 96, it / 96) == MochaIcons.INK.toInt() }
        assertTrue("ink pixels $ink", ink > 96)
    }

    @Test fun originalShapeFallsBackToADisc() {
        val bitmap = MochaIconRenderer.render(icon, 96, IconShape.ORIGINAL)
        assertEquals(0, Color.alpha(bitmap.getPixel(1, 1)))
        assertEquals(255, Color.alpha(bitmap.getPixel(48, 2)))
    }

    @Test fun dominantColorReadsTheIconsHue() {
        val red = MochaIconRenderer.dominantColor(ColorDrawable(Color.RED))
        assertEquals(MochaIcons.Tone.TERRACOTTA, MochaIcons.toneFor(red))
        val grey = MochaIconRenderer.dominantColor(ColorDrawable(Color.GRAY))
        assertEquals(MochaIcons.Tone.CLAY, MochaIcons.toneFor(grey))
    }

    private fun assertEquals(expected: Int, actual: Int, delta: Int) =
        assertTrue("$expected vs $actual", kotlin.math.abs(expected - actual) <= delta)
}
