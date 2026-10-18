package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withTranslation
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.design.GlassSpecs
import kotlin.math.pow

/**
 * The page backdrop: the base color plus one warm and one cool ambient glow. Static — it never
 * moves with the scroll (design section 7). Both gradients are cached per size, not per frame.
 */
@Composable
fun GlassPage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = GlassTheme.palette
    Box(
        modifier.fillMaxSize().drawWithCache {
            val warm = Brush.radialGradient(
                listOf(palette.warmGlow, Color.Transparent),
                center = Offset(size.width * GlassTokens.WARM_GLOW_X, size.height * GlassTokens.WARM_GLOW_Y),
                radius = size.height * GlassTokens.WARM_GLOW_RADIUS,
            )
            val cool = Brush.radialGradient(
                listOf(palette.coolGlow, Color.Transparent),
                center = Offset(size.width * GlassTokens.COOL_GLOW_X, size.height * GlassTokens.COOL_GLOW_Y),
                radius = size.height * GlassTokens.COOL_GLOW_RADIUS,
            )
            onDrawBehind {
                drawRect(palette.background)
                drawRect(warm)
                drawRect(cool)
            }
        },
        content = content,
    )
}

/**
 * A pane of glass: fill, a rim light that fades out over the top half, and an optional shadow.
 * Works for rows, cards, capsules and buttons, so every surface rises by light, not by lines.
 */
@Composable
fun Modifier.glassSurface(
    corner: Dp,
    capsule: Boolean = false,
    strong: Boolean = false,
    elevated: Boolean = false,
): Modifier {
    val palette = GlassTheme.palette
    val spec = GlassSpecs.Phone
    val shape = if (capsule) RoundedCornerShape(percent = 50) else RoundedCornerShape(corner)
    val lifted = if (elevated) Modifier.outerShadow(corner, capsule) else Modifier
    return this.then(lifted).clip(shape).drawWithCache {
        val fill = if (strong) palette.glassStrong else palette.glass
        val rimEnd = (size.height * spec.rimFadeFraction).coerceAtLeast(1f)
        val rim = Brush.verticalGradient(listOf(palette.rimLight, Color.Transparent), startY = 0f, endY = rimEnd)
        val cornerPx = if (capsule) size.height / 2f else corner.toPx()
        onDrawBehind {
            drawRect(fill)
            drawRim(rim, cornerPx, spec.rimWidthDp.dp.toPx())
        }
    }
}

/**
 * Shadow that stays outside the outline. `Modifier.shadow` paints behind the node, and glass is
 * translucent, so its blur would read as a grey rectangle inside the pane. The bands below are
 * clipped to the outside of the shape, so nothing can leak inwards.
 */
private fun Modifier.outerShadow(corner: Dp, capsule: Boolean): Modifier = drawWithCache {
    val spec = GlassSpecs.Phone
    val cornerPx = if (capsule) size.height / 2f else corner.toPx()
    val layers = GlassTokens.SHADOW_LAYERS
    val insets = GlassShadow.insets(GlassTokens.SHADOW_SPREAD_DP.dp.toPx(), layers)
    val bandAlpha = GlassShadow.bandAlpha(spec.shadowAlpha, layers)
    val drop = GlassTokens.SHADOW_DROP_DP.dp.toPx()
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = android.graphics.Color.BLACK
        alpha = bandAlpha
    }
    val outline = Path().apply { addRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, Path.Direction.CW) }
    val rect = RectF()
    onDrawWithContent {
        drawIntoCanvas { canvas ->
            // Overlapping bands accumulate into a smooth gradient; the clip keeps it outside.
            canvas.nativeCanvas.withTranslation(0f, drop) {
                clipPath(outline, Region.Op.DIFFERENCE)
                for (inset in insets) {
                    rect.set(inset, inset, size.width - inset, size.height - inset)
                    drawRoundRect(rect, cornerPx, cornerPx, paint)
                }
            }
        }
        drawContent()
    }
}

/**
 * Recipe of the raised-glass shadow. Every inset is negative, which is what keeps the shadow
 * out of the pane; [bandAlpha] is small enough that the overlapping bands accumulate to
 * [base] at the outline and fade to nothing at the spread.
 */
internal object GlassShadow {
    fun insets(spreadPx: Float, layers: Int): FloatArray =
        FloatArray(layers) { -(it + 1f) * (spreadPx / layers) }

    fun bandAlpha(base: Float, layers: Int): Int {
        val clamped = base.coerceIn(0f, 1f)
        if (clamped <= 0f) return 0
        val perBand = 1f - (1f - clamped).pow(1f / layers)
        return (perBand * 255f).toInt().coerceIn(1, 255)
    }
}

private fun DrawScope.drawRim(rim: Brush, cornerPx: Float, width: Float) {
    drawRoundRect(
        brush = rim,
        topLeft = Offset(width / 2f, width / 2f),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius(cornerPx),
        style = Stroke(width = width),
    )
}

/** A glass card with the standard page rhythm inside. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = GlassTokens.CARD_CORNER.dp,
    strong: Boolean = false,
    elevated: Boolean = false,
    padding: PaddingValues = PaddingValues(GlassTokens.CARD_PADDING.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.fillMaxWidth().glassSurface(corner, strong = strong, elevated = elevated).padding(padding),
        content = content,
    )
}
