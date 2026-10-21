package com.n2.oemlauncher.shared.model

import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutSettingsCompatTest {

    @Test
    fun v1ConfigWithoutDesignFieldsDecodesToDefaults() {
        val v1 = """{"revision":4,"layout":{"mode":"GRID","reduceMotion":true}}"""
        val layout = PayloadCodec.configFromJson(v1)!!.layout
        assertEquals(LayoutMode.GRID, layout.mode)
        assertEquals(LauncherDefaults.VISUAL_TIER, layout.visualTier)
        assertEquals(LauncherDefaults.ICON_STYLE, layout.iconStyle)
    }

    @Test
    fun unknownTierFromANewerPeerFallsBackToTheDefault() {
        val future = """{"layout":{"visualTier":"HOLOGRAPHIC","iconStyle":"THEMED"}}"""
        val layout = PayloadCodec.configFromJson(future)!!.layout
        assertEquals(LauncherDefaults.VISUAL_TIER, layout.visualTier)
        assertEquals(IconStyle.THEMED, layout.iconStyle)
    }

    @Test
    fun designFieldsRoundTrip() {
        val config = LauncherConfig(layout = LayoutSettings(visualTier = VisualTier.LITE, iconStyle = IconStyle.THEMED))
        assertEquals(config, PayloadCodec.configFromJson(PayloadCodec.configToJson(config)))
    }
}
