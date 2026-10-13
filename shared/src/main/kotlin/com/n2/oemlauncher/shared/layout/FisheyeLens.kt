package com.n2.oemlauncher.shared.layout

/**
 * Radial lens of the honeycomb: icons keep full size near the center and shrink towards the
 * rim. Radial positions follow the integral of the scale curve, so the gaps between neighbors
 * shrink together with the icons and the rim never shows holes. The mapping is strictly
 * increasing, so icons never cross each other.
 *
 * Distances are normalized by the viewport radius (0 = center, 1 = screen edge).
 */
class FisheyeLens(
    strength: Float,
    private val flatZone: Float = HoneycombDefaults.FLAT_ZONE,
    minScaleAtFullStrength: Float = HoneycombDefaults.MIN_SCALE_AT_FULL_STRENGTH,
    private val fadeStart: Float = HoneycombDefaults.FADE_START,
    private val fadeEnd: Float = HoneycombDefaults.FADE_END,
) {
    /** Scale reached at the screen edge and kept beyond it. */
    val minScale: Float
    private val shrink: Float
    private val transition: Float

    init {
        require(flatZone in 0f..MAX_FLAT_ZONE) { "flatZone out of range: $flatZone" }
        require(fadeEnd > fadeStart) { "fadeEnd must exceed fadeStart" }
        val clamped = strength.coerceIn(0f, 1f)
        minScale = 1f - clamped * (1f - minScaleAtFullStrength.coerceIn(0f, 1f))
        shrink = 1f - minScale
        transition = 1f - flatZone
    }

    /** Icon scale for an undistorted normalized distance [u]. */
    fun scaleAt(u: Float): Float = 1f - shrink * smoothstep(progress(u))

    /** Where an item at undistorted normalized distance [u] is drawn. */
    fun displacedDistance(u: Float): Float {
        if (u <= flatZone) return u
        val t = (u - flatZone) / transition
        if (t <= 1f) {
            val t3 = t * t * t
            return flatZone + transition * (t - shrink * (t3 - t3 * t / 2f))
        }
        val atRim = flatZone + transition * (1f - shrink / 2f)
        return atRim + (u - 1f) * minScale
    }

    /** Opacity for an icon whose outer edge sits at normalized distance [edgeU]. */
    fun alphaAt(edgeU: Float): Float = when {
        edgeU <= fadeStart -> 1f
        edgeU >= fadeEnd -> 0f
        else -> 1f - smoothstep((edgeU - fadeStart) / (fadeEnd - fadeStart))
    }

    private fun progress(u: Float): Float = ((u - flatZone) / transition).coerceIn(0f, 1f)

    private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)

    private companion object {
        const val MAX_FLAT_ZONE = 0.95f
    }
}
