package com.n2.oemlauncher.mobile.iconpack

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.core.graphics.PathParser
import com.n2.oemlauncher.shared.design.MochaGlyph
import com.n2.oemlauncher.shared.design.MochaIcons
import java.util.concurrent.ConcurrentHashMap

/**
 * The phone's copy of the watch's mocha icon drawing, for the demo watch and the preview: the
 * same tone, sheen and glyph proportions, painted unmasked so the caller's shape applies.
 */
object MochaIconArt {
    private val glyphs = ConcurrentHashMap<MochaGlyph, Path>()

    fun draw(canvas: Canvas, side: Int, icon: MochaIcons.Icon) {
        canvas.drawColor(icon.tone.argb.toInt())
        val sheen = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, side * MochaIcons.SHEEN_FRACTION,
                MochaIcons.SHEEN_COLOR, Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, side.toFloat(), side.toFloat(), sheen)
        val source = glyphs.getOrPut(icon.glyph) { PathParser.createPathFromPathData(icon.glyph.pathData) }
        val scale = side * MochaIcons.GLYPH_RATIO / MochaIcons.VIEWPORT
        val inset = (side - MochaIcons.VIEWPORT * scale) / 2f
        val matrix = Matrix().apply {
            setScale(scale, scale)
            // The path data's y axis runs from -VIEWPORT to 0.
            postTranslate(inset, inset + MochaIcons.VIEWPORT * scale)
        }
        val path = Path(source).apply { transform(matrix) }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MochaIcons.INK.toInt() })
    }
}
