package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A recessed glass track that fills with champagne when on, and a liquid knob. Tap to flip
 * it, or drag the knob across: it swells into a lens under the finger and lands on whichever
 * side it is let go nearer to.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = GlassTheme.palette
    val onBrush = remember(palette) { Brush.verticalGradient(listOf(palette.accentLight, palette.accentDeep)) }
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    val press = rememberKnobPress()
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(checked)
    val change by rememberUpdatedState(onCheckedChange)
    val target = if (checked) 1f else 0f
    LaunchedEffect(target) {
        if (!dragging && progress.targetValue != target) {
            progress.animateTo(target, spring(GlassTokens.SPRING_DAMPING, GlassTokens.SPRING_STIFFNESS))
        }
    }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect {
            when (it) {
                is PressInteraction.Press -> press.press()
                // A drag cancels the tap; the knob stays swollen until the drag lets go.
                is PressInteraction.Release, is PressInteraction.Cancel -> if (!dragging) press.release()
            }
        }
    }
    val travel = GlassTokens.SWITCH_WIDTH - GlassTokens.SWITCH_KNOB_WIDTH - GlassTokens.SWITCH_INSET * 2
    val track = rememberLayerBackdrop()
    Box(
        modifier.height(UiDimensions.TOUCH_TARGET.dp)
            .toggleable(
                value = checked, interactionSource = interaction, indication = null,
                role = Role.Switch, onValueChange = onCheckedChange,
            )
            .pointerInput(Unit) {
                val travelPx = travel.dp.toPx()
                fun settle() {
                    val on = progress.value >= HALF
                    dragging = false
                    scope.launch {
                        progress.animateTo(if (on) 1f else 0f, spring(GlassTokens.SPRING_DAMPING, GlassTokens.SPRING_STIFFNESS))
                    }
                    press.release()
                    if (on != current) change(on)
                }
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true; press.press() },
                    onDragEnd = ::settle,
                    onDragCancel = ::settle,
                ) { pointer, dx ->
                    pointer.consume()
                    scope.launch { progress.snapTo((progress.value + dx / travelPx).coerceIn(0f, 1f)) }
                }
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.size(GlassTokens.SWITCH_WIDTH.dp, GlassTokens.SWITCH_HEIGHT.dp)
                .layerBackdrop(track)
                .glassSurface(corner = 0.dp, capsule = true, strong = true, recessed = true),
        ) {
            Box(Modifier.fillMaxSize().clip(Capsule()).background(onBrush, alpha = progress.value))
        }
        Box(
            Modifier
                .offset { IntOffset(((GlassTokens.SWITCH_INSET + travel * progress.value).dp.toPx()).roundToInt(), 0) }
                .size(GlassTokens.SWITCH_KNOB_WIDTH.dp, GlassTokens.SWITCH_KNOB_HEIGHT.dp)
                .liquidKnob(press, rememberCombinedBackdrop(LocalGlassBackdrop.current, track)),
        )
    }
}

private const val HALF = 0.5f
