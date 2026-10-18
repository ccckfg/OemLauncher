package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme

/** Label, live value and a champagne-filled slider, all on one glass card. */
@Composable
fun GlassSliderCard(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier,
        padding = PaddingValues(
            start = GlassTokens.CARD_PADDING.dp,
            end = GlassTokens.CARD_PADDING.dp,
            top = GlassTokens.CARD_PADDING.dp,
            bottom = 0.dp,
        ),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label, Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    valueText, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlassSlider(value, valueRange, onValueChange, onValueChangeFinished, description)
        }
    }
}

/** Slider with the design's own track and knob; semantics keep it usable without gestures. */
@Composable
fun GlassSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val palette = GlassTheme.palette
    val fill = remember(palette) {
        Brush.horizontalGradient(listOf(palette.accentDeep, palette.accentLight))
    }
    Box(
        modifier.fillMaxWidth().height(UiDimensions.TOUCH_TARGET.dp)
            .pointerInput(valueRange) {
                val thumb = GlassTokens.SLIDER_THUMB.dp.toPx()
                detectDragGestures(
                    onDragStart = { offset -> onValueChange(valueAt(offset.x, size.width.toFloat(), thumb, valueRange)) },
                    onDragEnd = onValueChangeFinished,
                    onDragCancel = onValueChangeFinished,
                    onDrag = { change, _ ->
                        change.consume()
                        onValueChange(valueAt(change.position.x, size.width.toFloat(), thumb, valueRange))
                    },
                )
            }
            .pointerInput(valueRange) {
                val thumb = GlassTokens.SLIDER_THUMB.dp.toPx()
                detectTapGestures { offset ->
                    onValueChange(valueAt(offset.x, size.width.toFloat(), thumb, valueRange))
                    onValueChangeFinished()
                }
            }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    onValueChange(target)
                    onValueChangeFinished()
                    true
                }
            }
            .drawBehind {
                val trackHeight = GlassTokens.SLIDER_TRACK.dp.toPx()
                val thumb = GlassTokens.SLIDER_THUMB.dp.toPx()
                val centerY = size.height / 2f
                val top = centerY - trackHeight / 2f
                val startX = thumb / 2f
                val travel = (size.width - thumb).coerceAtLeast(1f)
                val fraction = fractionOf(value, valueRange)
                drawRoundRect(
                    color = palette.glassStrong,
                    topLeft = Offset(startX, top),
                    size = Size(travel, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f),
                )
                drawRoundRect(
                    brush = fill,
                    topLeft = Offset(startX, top),
                    size = Size(travel * fraction, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f),
                )
                val thumbX = startX + travel * fraction
                drawCircle(color = palette.accentLight, radius = thumb / 2f, center = Offset(thumbX, centerY))
                drawCircle(
                    color = palette.accent,
                    radius = thumb / 2f - 0.5.dp.toPx(),
                    center = Offset(thumbX, centerY),
                    style = Stroke(width = 1.dp.toPx()),
                )
            },
    )
}

/** Share of [range] covered by [value], clamped to 0..1. */
fun fractionOf(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f) return 0f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}

private fun valueAt(x: Float, width: Float, thumbPx: Float, range: ClosedFloatingPointRange<Float>): Float {
    val usable = (width - thumbPx).coerceAtLeast(1f)
    val fraction = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
    return range.start + (range.endInclusive - range.start) * fraction
}

/** A label with a control on the right, inside the surrounding glass card. */
@Composable
fun GlassSettingRow(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().height(GlassTokens.SETTING_ROW.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        Text(
            label, Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
        )
        trailing()
    }
}
