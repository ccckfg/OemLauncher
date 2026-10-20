package com.n2.oemlauncher.wear.ui

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n2.oemlauncher.shared.design.LabelZone
import com.n2.oemlauncher.shared.design.Palettes
import com.n2.oemlauncher.wear.config.WearUiTokens
import kotlin.math.PI

/** Text, path, metrics and both paints change only with the label, density or viewport. */
internal class FocusLabelDrawing(
    text: String,
    width: Float,
    height: Float,
    fontPx: Float,
    outlinePx: Float,
) {
    val fill = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Palettes.Mocha.textSecondary.toInt()
        textSize = fontPx
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val outline = TextPaint(fill).apply {
        style = Paint.Style.STROKE
        strokeWidth = outlinePx
        strokeJoin = Paint.Join.ROUND
        color = Palettes.Mocha.background.toInt()
        alpha = (LabelZone.OUTLINE_ALPHA * 255).toInt()
    }
    private val radius = (minOf(width, height) / 2f - outlinePx / 2f - fill.descent()).coerceAtLeast(0f)
    private val sweep = WearUiTokens.FOCUS_LABEL_SWEEP_DEGREES
    private val length = radius * sweep * PI.toFloat() / 180f
    val label = TextUtils.ellipsize(text, fill, length, TextUtils.TruncateAt.END).toString()
    val offset = ((length - fill.measureText(label)) / 2f).coerceAtLeast(0f)
    val path = Path().apply {
        addArc(
            RectF(width / 2f - radius, height / 2f - radius, width / 2f + radius, height / 2f + radius),
            90f + sweep / 2f,
            -sweep,
        )
    }

    fun draw(canvas: android.graphics.Canvas) {
        canvas.drawTextOnPath(label, path, offset, 0f, outline)
        canvas.drawTextOnPath(label, path, offset, 0f, fill)
    }
}

@Composable
fun HoneycombFocusLabel(text: String, visible: Boolean, alpha: () -> Float = { 1f }) {
    if (!visible) return
    Spacer(
        Modifier.fillMaxSize()
            .clearAndSetSemantics {}
            // A fading full-screen layer would be drawn offscreen every frame of the entrance,
            // which doubled the GPU time on the watch; the label is two paths of text, so the
            // alpha can go straight onto its draw calls.
            .graphicsLayer {
                this.alpha = alpha()
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .drawWithCache {
                val drawing = FocusLabelDrawing(
                    text, size.width, size.height,
                    WearUiTokens.FOCUS_LABEL_SP.sp.toPx(),
                    LabelZone.OUTLINE_WIDTH_DP.dp.toPx(),
                )
                onDrawBehind { drawing.draw(drawContext.canvas.nativeCanvas) }
            },
    )
}
