package com.n2.oemlauncher.mobile

import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.ui.components.GlassShadow
import com.n2.oemlauncher.shared.design.GlassSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Raised glass must never paint anything under the pane: the fill is translucent, so a shadow
 * behind it reads as a grey rectangle with square corners inside the card. Regression for the
 * artefact seen in `.screenshots/m4/home-latte.png`.
 */
class GlassShadowTest {
    @Test fun everyShadowBandStaysOutsideTheOutline() {
        val insets = GlassShadow.insets(GlassTokens.SHADOW_SPREAD_DP.toFloat(), GlassTokens.SHADOW_LAYERS)
        assertEquals(GlassTokens.SHADOW_LAYERS, insets.size)
        for (inset in insets) assertTrue("band at $inset reaches inside the shape", inset <= 0f)
    }

    @Test fun bandsGrowOutwardsAndStopAtTheSpread() {
        val spread = GlassTokens.SHADOW_SPREAD_DP.toFloat()
        val insets = GlassShadow.insets(spread, GlassTokens.SHADOW_LAYERS)
        val step = spread / GlassTokens.SHADOW_LAYERS
        for (layer in 1 until insets.size) {
            assertTrue(insets[layer] < insets[layer - 1])
            assertEquals(step, insets[layer - 1] - insets[layer], 1e-4f)
        }
        assertEquals(-step, insets.first(), 1e-4f)
        assertEquals(-spread, insets.last(), 1e-4f)
    }

    /**
     * The bands overlap, so the darkness at the outline is the accumulation of all of them.
     * That accumulation has to land on the spec's shadow alpha, or the card is either flat or
     * smudged; and it has to stay under 1 so the blur never turns into a solid band.
     */
    @Test fun bandAlphaAccumulatesToTheSpecShadow() {
        val layers = GlassTokens.SHADOW_LAYERS
        val alpha = GlassShadow.bandAlpha(GlassSpecs.Phone.shadowAlpha, layers) / 255f
        val accumulated = 1f - (1f - alpha).pow(layers)
        assertEquals(GlassSpecs.Phone.shadowAlpha.toDouble(), accumulated.toDouble(), 0.02)
        assertTrue(alpha > 0f && alpha < GlassSpecs.Phone.shadowAlpha)
        assertEquals(0, GlassShadow.bandAlpha(0f, layers))
    }

    @Test fun aLiteTierWouldDrawNothing() {
        assertEquals(0, GlassShadow.bandAlpha(GlassSpecs.WatchLite.shadowAlpha, GlassTokens.SHADOW_LAYERS))
    }
}
