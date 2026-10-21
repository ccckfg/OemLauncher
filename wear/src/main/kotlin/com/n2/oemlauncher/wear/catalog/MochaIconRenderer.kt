package com.n2.oemlauncher.wear.catalog

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.n2.oemlauncher.shared.design.MochaGlyph
import com.n2.oemlauncher.shared.design.MochaIcons
import com.n2.oemlauncher.shared.model.IconShape
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the built-in mocha icons: an earth-tone disc with a little sheen and a cream glyph.
 * Runs on the catalog's IO dispatcher like the rest of the bitmap work.
 */
internal object MochaIconRenderer {
    private val glyphs = ConcurrentHashMap<MochaGlyph, Path>()

    /** The pack's own drawing of an app. */
    fun render(icon: MochaIcons.Icon, size: Int, shape: IconShape): Bitmap = disc(size, shape, icon.tone) { canvas ->
        val source = glyphs.getOrPut(icon.glyph) { PathParser.createPathFromPathData(icon.glyph.pathData) }
        val scale = size * MochaIcons.GLYPH_RATIO / MochaIcons.VIEWPORT
        val inset = (size - MochaIcons.VIEWPORT * scale) / 2f
        val matrix = Matrix().apply {
            setScale(scale, scale)
            // The path data's y axis runs from -VIEWPORT to 0.
            postTranslate(inset, inset + MochaIcons.VIEWPORT * scale)
        }
        val path = Path(source).apply { transform(matrix) }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MochaIcons.INK.toInt() })
    }

    /**
     * An app the pack does not draw but that ships a monochrome layer: the same disc, toned
     * after the app's own colors, with that layer in cream.
     */
    fun monochrome(icon: Drawable, layer: Drawable, size: Int, shape: IconShape): Bitmap =
        disc(size, shape, MochaIcons.toneFor(dominantColor(icon))) { canvas ->
            layer.setTint(MochaIcons.INK.toInt())
            layer.setBounds(0, 0, size, size)
            layer.draw(canvas)
        }

    private inline fun disc(size: Int, shape: IconShape, tone: MochaIcons.Tone, glyph: (Canvas) -> Unit): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        // A pack icon has no "original" outline of its own, so that shape falls back to a disc.
        canvas.clipPath(IconBitmapRenderer.maskPath(size, if (shape == IconShape.ORIGINAL) IconShape.CIRCLE else shape))
        canvas.drawColor(tone.argb.toInt())
        val sheen = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, size * MochaIcons.SHEEN_FRACTION,
                MochaIcons.SHEEN_COLOR, Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), sheen)
        glyph(canvas)
        return bitmap
    }

    /**
     * The icon's color as the eye reads it: opaque pixels averaged with colorful ones weighted
     * up, so a white plate around a red mark still reads red.
     */
    fun dominantColor(icon: Drawable): Int {
        val sample = icon.toBitmap(SAMPLE, SAMPLE, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(SAMPLE * SAMPLE).also { sample.getPixels(it, 0, SAMPLE, 0, 0, SAMPLE, SAMPLE) }
        var red = 0f
        var green = 0f
        var blue = 0f
        var total = 0f
        pixels.forEach { pixel ->
            if (Color.alpha(pixel) < OPAQUE) return@forEach
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val weight = (max(r, max(g, b)) - min(r, min(g, b))) / CHANNEL + NEUTRAL_WEIGHT
            red += r * weight
            green += g * weight
            blue += b * weight
            total += weight
        }
        if (total <= 0f) return Color.GRAY
        return Color.rgb((red / total).toInt(), (green / total).toInt(), (blue / total).toInt())
    }

    private const val SAMPLE = 12
    private const val OPAQUE = 128
    private const val CHANNEL = 255f
    /** Keeps an all-grey icon averaging to grey instead of dividing by nothing. */
    private const val NEUTRAL_WEIGHT = 0.05f
}
