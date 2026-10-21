package com.n2.oemlauncher.shared.design

import com.n2.oemlauncher.shared.model.VisualTier

/**
 * How glass surfaces and motion are rendered for one visual tier. The watch picks a spec from
 * the synced [VisualTier]; the phone preview reads the same spec so it draws what the watch draws.
 */
data class GlassSpec(
    /** Live blur of the content behind an overlay; 0 means a dim scrim instead of blur. */
    val overlayBlurDp: Float,
    /** Warm and cool glows behind content; false means a flat [DesignPalette.background]. */
    val ambientGlow: Boolean,
    val rimWidthDp: Float,
    /** The rim light fades to nothing at this fraction of the surface height. */
    val rimFadeFraction: Float,
    /** Soft drop shadow under raised glass; 0 disables it. */
    val shadowAlpha: Float,
    /** Scale and fade transitions; false snaps between states. */
    val animated: Boolean,
)

object GlassSpecs {
    val WatchRefined = GlassSpec(
        overlayBlurDp = 12f,
        ambientGlow = true,
        rimWidthDp = 1f,
        rimFadeFraction = 0.55f,
        shadowAlpha = 0.35f,
        animated = true,
    )

    /** Same colors and shapes as refined; no blur, glow, shadow or animation. */
    val WatchLite = GlassSpec(
        overlayBlurDp = 0f,
        ambientGlow = false,
        rimWidthDp = 1f,
        rimFadeFraction = 0.55f,
        shadowAlpha = 0f,
        animated = false,
    )

    val Phone = GlassSpec(
        overlayBlurDp = 24f,
        ambientGlow = true,
        rimWidthDp = 1f,
        rimFadeFraction = 0.5f,
        shadowAlpha = 0.28f,
        animated = true,
    )

    fun watch(tier: VisualTier): GlassSpec = when (tier) {
        VisualTier.REFINED -> WatchRefined
        VisualTier.LITE -> WatchLite
    }
}
