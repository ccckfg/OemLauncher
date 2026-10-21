package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import kotlin.math.roundToInt

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
        LabeledSlider(label, value, valueText, valueRange, description, onValueChange, onValueChangeFinished)
    }
}

/** The slider with its label and live value, for use inside a card that holds other settings. */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
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

/**
 * Slider with a recessed glass track, a champagne fill and a liquid knob that swells into a
 * lens while dragged; semantics keep it usable without gestures.
 */
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
    val fill = remember(palette) { Brush.horizontalGradient(listOf(palette.accentDeep, palette.accentLight)) }
    val fraction by animateFloatAsState(
        fractionOf(value, valueRange),
        spring(stiffness = KNOB_FOLLOW_STIFFNESS),
        label = "slider",
    )
    val press = rememberKnobPress()
    val track = rememberLayerBackdrop()
    // The gesture handlers outlive recompositions; always call the latest callbacks.
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    BoxWithConstraints(
        modifier.fillMaxWidth().height(UiDimensions.TOUCH_TARGET.dp)
            .pointerInput(valueRange) {
                val knob = GlassTokens.SLIDER_KNOB_WIDTH.dp.toPx()
                detectDragGestures(
                    onDragStart = { offset ->
                        press.press()
                        change(valueAt(offset.x, size.width.toFloat(), knob, valueRange))
                    },
                    onDragEnd = { press.release(); finished() },
                    onDragCancel = { press.release(); finished() },
                    onDrag = { pointer, _ ->
                        pointer.consume()
                        change(valueAt(pointer.position.x, size.width.toFloat(), knob, valueRange))
                    },
                )
            }
            .pointerInput(valueRange) {
                val knob = GlassTokens.SLIDER_KNOB_WIDTH.dp.toPx()
                detectTapGestures(
                    onPress = { press.press(); tryAwaitRelease(); press.release() },
                    onTap = { offset ->
                        change(valueAt(offset.x, size.width.toFloat(), knob, valueRange))
                        finished()
                    },
                )
            }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    onValueChange(target)
                    onValueChangeFinished()
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val knobWidth = GlassTokens.SLIDER_KNOB_WIDTH.dp
        val travel = maxWidth - knobWidth
        // The track runs between the knob's centers at both ends, so the fill meets the knob.
        Box(
            Modifier.padding(horizontal = knobWidth / 2).fillMaxWidth().height(GlassTokens.SLIDER_TRACK.dp)
                .layerBackdrop(track)
                .glassSurface(corner = 0.dp, capsule = true, strong = true, recessed = true),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(Capsule()).background(fill))
        }
        Box(
            Modifier
                .offset { IntOffset((travel.toPx() * fraction).roundToInt(), 0) }
                .size(knobWidth, GlassTokens.SLIDER_KNOB_HEIGHT.dp)
                .liquidKnob(press, rememberCombinedBackdrop(LocalGlassBackdrop.current, track)),
        )
    }
}

/** Share of [range] covered by [value], clamped to 0..1. */
fun fractionOf(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f) return 0f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}

private fun valueAt(x: Float, width: Float, knobPx: Float, range: ClosedFloatingPointRange<Float>): Float {
    val usable = (width - knobPx).coerceAtLeast(1f)
    val fraction = ((x - knobPx / 2f) / usable).coerceIn(0f, 1f)
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

/** The knob trails the finger on a stiff spring: it reads as liquid, never as lag. */
private const val KNOB_FOLLOW_STIFFNESS = 1000f
