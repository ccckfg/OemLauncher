package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.n2.oemlauncher.mobile.config.GlassTokens

/**
 * The two-lobe rim light of liquid glass as a plain gradient stroke: bright where the light
 * from the top left strikes, again (a little weaker) at the opposite corner, faint between.
 * The backdrop library draws this with a runtime shader instead, but compiles one per pane on
 * the main thread, which held a page's first frame for over half a second and swallowed the
 * page transition. The outline and brush are rebuilt only when the size or color changes.
 */
internal class SpecularRim {
    private var size = Size.Unspecified
    private var color = Color.Unspecified
    private var outline: Outline? = null
    private var brush: Brush? = null
    private var stroke = Stroke(0f)

    fun draw(scope: DrawScope, shape: Shape, color: Color, width: Float) = with(scope) {
        if (size != this@SpecularRim.size || color != this@SpecularRim.color || outline == null) {
            this@SpecularRim.size = size
            this@SpecularRim.color = color
            outline = shape.createOutline(size, layoutDirection, this)
            // Project the rectangle onto the light's 45-degree axis, so the lobes sit on the corners.
            val reach = (size.width + size.height) / 4f
            val center = Offset(size.width / 2f, size.height / 2f)
            val faint = color.copy(alpha = color.alpha * GlassTokens.RIM_FAINT)
            brush = Brush.linearGradient(
                0f to color,
                GlassTokens.RIM_LOBE to faint,
                1f - GlassTokens.RIM_LOBE to faint,
                1f to color.copy(alpha = color.alpha * GlassTokens.RIM_BACK_LOBE),
                start = center - Offset(reach, reach),
                end = center + Offset(reach, reach),
            )
        }
        // The pane clips to its shape, so only the inner half of a centered stroke shows.
        if (stroke.width != width * 2f) stroke = Stroke(width * 2f)
        drawOutline(outline!!, brush!!, style = stroke, blendMode = BlendMode.Plus)
    }
}
