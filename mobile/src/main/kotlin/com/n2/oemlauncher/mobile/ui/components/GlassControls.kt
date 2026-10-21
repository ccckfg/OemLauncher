package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Section headers are small champagne caps: the only place [GlassPalette.accentText] is text. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(start = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = GlassTheme.palette.accentText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Segmented choice: a recessed glass track and one champagne pill, the control's only
 * champagne fill. Tap a segment and the pill springs over; or take hold of the track and drag,
 * and the pill swells into a lens under the finger and settles on the nearest segment. Inside
 * a glass card pass [strong], so the track reads as glass on glass.
 */
@Composable
fun <T> GlassSegmented(
    options: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    description: String? = null,
    strong: Boolean = false,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val palette = GlassTheme.palette
    val champagne = remember(palette) { Brush.verticalGradient(listOf(palette.accentLight, palette.accentDeep)) }
    val count = options.size.coerceAtLeast(1)
    val index = options.indexOf(selected).coerceAtLeast(0)
    val position = remember { Animatable(index.toFloat()) }
    val press = rememberKnobPress()
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val choices by rememberUpdatedState(options)
    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(index) {
        // A drag already springs the pill to where it was let go.
        if (dragging || position.targetValue == index.toFloat()) return@LaunchedEffect
        press.press()
        position.animateTo(index.toFloat(), spring(GlassTokens.SPRING_DAMPING, GlassTokens.SPRING_STIFFNESS))
        press.release()
    }
    val track = rememberLayerBackdrop()
    BoxWithConstraints(
        modifier.fillMaxWidth().height(GlassTokens.SEGMENT_HEIGHT.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        // The track is a sibling of the pill, so the pill can refract it without seeing itself.
        Box(
            Modifier.fillMaxSize().layerBackdrop(track)
                .glassSurface(corner = 0.dp, capsule = true, strong = strong, recessed = true),
        )
        val inner = maxWidth - (GlassTokens.SEGMENT_PADDING * 2).dp
        val segment = inner / count
        Box(
            Modifier.fillMaxSize().padding(GlassTokens.SEGMENT_PADDING.dp)
                .pointerInput(count) {
                    val step = size.width / count.toFloat()
                    fun settle() {
                        val target = position.value.roundToInt().coerceIn(0, count - 1)
                        dragging = false
                        scope.launch {
                            position.animateTo(target.toFloat(), spring(GlassTokens.SPRING_DAMPING, GlassTokens.SPRING_STIFFNESS))
                            press.release()
                        }
                        choices.getOrNull(target)?.let(select)
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true; press.press() },
                        onDragEnd = ::settle,
                        onDragCancel = ::settle,
                    ) { change, dx ->
                        change.consume()
                        scope.launch { position.snapTo((position.value + dx / step).coerceIn(0f, count - 1f)) }
                    }
                },
        ) {
            Box(
                Modifier.width(segment).fillMaxHeight()
                    .offset { IntOffset((segment.toPx() * position.value).roundToInt(), 0) }
                    .liquidKnob(
                        press, rememberCombinedBackdrop(LocalGlassBackdrop.current, track),
                        fill = champagne, pressedScale = GlassTokens.PILL_PRESSED_SCALE,
                    ),
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, option ->
                    // Held, the pill clears into a lens, so the label under it stays light.
                    val cover = (1f - abs(position.value - i)).coerceIn(0f, 1f) * (1f - press.value)
                    Segment(i == index, label(option), cover) { onSelect(option) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Segment(selected: Boolean, text: String, cover: Float, onSelect: () -> Unit) {
    val palette = GlassTheme.palette
    Box(
        Modifier.weight(1f).fillMaxHeight().clip(Capsule())
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            // The label darkens exactly as much as the pill covers it while it slides.
            color = lerp(palette.textSecondary, palette.onAccent, cover),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
