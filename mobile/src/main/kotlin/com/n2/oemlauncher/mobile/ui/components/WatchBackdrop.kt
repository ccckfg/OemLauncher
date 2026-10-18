package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.n2.oemlauncher.mobile.config.PreviewTokens
import com.n2.oemlauncher.shared.design.GlassSpecs
import com.n2.oemlauncher.shared.design.Palettes
import com.n2.oemlauncher.shared.model.VisualTier
import kotlin.math.hypot

/**
 * The watch backdrop of the refined tier: the base color, a warm glow above the center and a
 * cool one at the lower right, then an edge vignette drawn on top of the icons. The lite tier
 * draws a flat background, so no gradient is even created for it.
 */
class WatchBackdrop(width: Float, height: Float, tier: VisualTier) {
    private val spec = GlassSpecs.watch(tier)
    private val palette = Palettes.Mocha
    private val width = width
    private val height = height
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** True when this tier paints ambient light; the phone mirrors the watch's choice. */
    val ambientGlow: Boolean = spec.ambientGlow

    /** How many ambient shaders exist; 0 means the lite tier never built any. */
    val glowShaderCount: Int

    private val warm: RadialGradient?
    private val cool: RadialGradient?
    private val vignette: RadialGradient?

    init {
        if (ambientGlow) {
            // Stops and centers are the ones in the visual sample (Main.dc.html): its CSS
            // radial gradients size to the farthest corner, so the same radius is used here.
            warm = RadialGradient(
                width / 2f, height * PreviewTokens.WARM_GLOW_Y, farthestCorner(width / 2f, height * PreviewTokens.WARM_GLOW_Y),
                intArrayOf(
                    withAlpha(opaque(palette.backgroundGlow), PreviewTokens.WARM_GLOW_ALPHA),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, PreviewTokens.WARM_GLOW_STOP),
                Shader.TileMode.CLAMP,
            )
            cool = RadialGradient(
                width * PreviewTokens.COOL_GLOW_X, height * PreviewTokens.COOL_GLOW_Y,
                farthestCorner(width * PreviewTokens.COOL_GLOW_X, height * PreviewTokens.COOL_GLOW_Y),
                intArrayOf(
                    withAlpha(opaque(palette.coolGlow), PreviewTokens.COOL_GLOW_ALPHA),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, PreviewTokens.COOL_GLOW_STOP),
                Shader.TileMode.CLAMP,
            )
            vignette = RadialGradient(
                width / 2f, height / 2f, farthestCorner(width / 2f, height / 2f),
                intArrayOf(
                    Color.TRANSPARENT, Color.TRANSPARENT,
                    withAlpha(Color.BLACK, PreviewTokens.VIGNETTE_ALPHA),
                ),
                floatArrayOf(0f, PreviewTokens.VIGNETTE_START_FRACTION, 1f),
                Shader.TileMode.CLAMP,
            )
            glowShaderCount = GLOW_SHADERS
        } else {
            warm = null
            cool = null
            vignette = null
            glowShaderCount = 0
        }
    }

    private fun farthestCorner(cx: Float, cy: Float): Float {
        val dx = maxOf(cx, width - cx)
        val dy = maxOf(cy, height - cy)
        return hypot(dx, dy)
    }

    fun drawBackground(canvas: Canvas) {
        canvas.drawColor(opaque(palette.background))
        warm?.let { paint.shader = it; canvas.drawRect(0f, 0f, width, height, paint) }
        cool?.let { paint.shader = it; canvas.drawRect(0f, 0f, width, height, paint) }
        paint.shader = null
    }

    /** Drawn after the icons so they sink into the bezel instead of being cut by it. */
    fun drawVignette(canvas: Canvas) {
        val shader = vignette ?: return
        paint.shader = shader
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    private fun opaque(color: Long): Int = Color.argb(0xFF, red(color), green(color), blue(color))

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255f).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun red(color: Long) = ((color shr 16) and 0xFF).toInt()
    private fun green(color: Long) = ((color shr 8) and 0xFF).toInt()
    private fun blue(color: Long) = (color and 0xFF).toInt()

    private companion object {
        const val GLOW_SHADERS = 2
    }
}
