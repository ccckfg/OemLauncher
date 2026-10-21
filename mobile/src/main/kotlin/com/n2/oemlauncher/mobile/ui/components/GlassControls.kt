package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
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
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
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
 * Segmented choice: a recessed glass track and one champagne glass pill that slides on a
 * spring to the selected segment — the only champagne fill of the control. Inside a glass card
 * pass [strong], so the track reads as glass on glass.
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
    val index = options.indexOf(selected).coerceAtLeast(0)
    val position by animateFloatAsState(
        index.toFloat(),
        spring(GlassTokens.SPRING_DAMPING, GlassTokens.SPRING_STIFFNESS),
        label = "segment",
    )
    BoxWithConstraints(
        modifier.fillMaxWidth().height(GlassTokens.SEGMENT_HEIGHT.dp)
            .glassSurface(corner = 0.dp, capsule = true, strong = strong, recessed = true)
            .padding(GlassTokens.SEGMENT_PADDING.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        Box(
            Modifier.width(segment).fillMaxHeight()
                .offset { IntOffset((segment.toPx() * position).roundToInt(), 0) }
                .glassSurface(corner = 0.dp, capsule = true, champagne = true),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, option ->
                val cover = (1f - abs(position - i)).coerceIn(0f, 1f)
                Segment(i == index, label(option), cover) { onSelect(option) }
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

/**
 * A recessed glass track that fills with champagne when on, and a liquid knob that springs
 * across and swells into a lens while held.
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
    val progress by animateFloatAsState(
        if (checked) 1f else 0f,
        spring(GlassTokens.SPRING_DAMPING, GlassTokens.SPRING_STIFFNESS),
        label = "switch",
    )
    val interaction = remember { MutableInteractionSource() }
    val press = rememberKnobPress()
    LaunchedEffect(interaction) {
        interaction.interactions.collect {
            when (it) {
                is PressInteraction.Press -> press.press()
                is PressInteraction.Release, is PressInteraction.Cancel -> press.release()
            }
        }
    }
    val track = rememberLayerBackdrop()
    Box(
        modifier.height(UiDimensions.TOUCH_TARGET.dp)
            .toggleable(
                value = checked, interactionSource = interaction, indication = null,
                role = Role.Switch, onValueChange = onCheckedChange,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.size(GlassTokens.SWITCH_WIDTH.dp, GlassTokens.SWITCH_HEIGHT.dp)
                .layerBackdrop(track)
                .glassSurface(corner = 0.dp, capsule = true, strong = true, recessed = true),
        ) {
            Box(Modifier.fillMaxSize().clip(Capsule()).background(onBrush, alpha = progress))
        }
        val travel = GlassTokens.SWITCH_WIDTH - GlassTokens.SWITCH_KNOB_WIDTH - GlassTokens.SWITCH_INSET * 2
        Box(
            Modifier
                .offset { IntOffset(((GlassTokens.SWITCH_INSET + travel * progress).dp.toPx()).roundToInt(), 0) }
                .size(GlassTokens.SWITCH_KNOB_WIDTH.dp, GlassTokens.SWITCH_KNOB_HEIGHT.dp)
                .liquidKnob(press, rememberCombinedBackdrop(LocalGlassBackdrop.current, track)),
        )
    }
}
