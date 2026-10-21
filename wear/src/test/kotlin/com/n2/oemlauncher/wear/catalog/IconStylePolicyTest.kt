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

    @Test fun overrideWinsOverThemedAndDefaultIcons() {
        IconStyle.entries.forEach { style ->
            assertEquals(IconRenderMode.OVERRIDE, iconRenderMode(style, true, overrideAvailable = true))
            assertEquals(IconRenderMode.OVERRIDE, iconRenderMode(style, false, overrideAvailable = true))
        }
    }

    @Test fun unavailableOverrideFallsThroughToThemedBeforeDefault() {
        assertEquals(IconRenderMode.THEMED, iconRenderMode(IconStyle.THEMED, true, overrideAvailable = false))
        assertEquals(IconRenderMode.ORIGINAL, iconRenderMode(IconStyle.THEMED, false, overrideAvailable = false))
    }

    @Test fun packComesBeforeTheMonochromeLayerButNeverBeforeAnOverride() {
        assertEquals(IconRenderMode.PACK, iconRenderMode(IconStyle.THEMED, true, inPack = true))
        assertEquals(IconRenderMode.PACK, iconRenderMode(IconStyle.THEMED, false, inPack = true))
        assertEquals(IconRenderMode.OVERRIDE, iconRenderMode(IconStyle.THEMED, false, overrideAvailable = true, inPack = true))
        assertEquals(IconRenderMode.ORIGINAL, iconRenderMode(IconStyle.ORIGINAL, false, inPack = true))
    }
}
