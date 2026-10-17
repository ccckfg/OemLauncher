package com.n2.oemlauncher.wear.catalog

import com.n2.oemlauncher.shared.model.IconStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class IconStylePolicyTest {
    @Test fun themedStyleUsesMonochromeLayerWhenAvailable() {
        assertEquals(IconRenderMode.THEMED, iconRenderMode(IconStyle.THEMED, true))
    }

    @Test fun themedStyleFallsBackToOriginalWithoutMonochromeLayer() {
        assertEquals(IconRenderMode.ORIGINAL, iconRenderMode(IconStyle.THEMED, false))
        assertEquals(IconRenderMode.ORIGINAL, iconRenderMode(IconStyle.ORIGINAL, true))
    }
}
