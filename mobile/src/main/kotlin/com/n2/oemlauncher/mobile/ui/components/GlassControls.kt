package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme

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

/** Segmented choice on a glass capsule; the selected segment is the only champagne fill. */
@Composable
fun <T> GlassSegmented(
    options: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    description: String? = null,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val palette = GlassTheme.palette
    val brush = remember(palette) { Brush.verticalGradient(listOf(palette.accentLight, palette.accentDeep)) }
    Row(
        modifier.fillMaxWidth().height(GlassTokens.SEGMENT_HEIGHT.dp)
            .glassSurface(corner = 0.dp, capsule = true)
            .padding(GlassTokens.SEGMENT_PADDING.dp)
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(GlassTokens.SEGMENT_GAP.dp),
    ) {
        options.forEach { option -> Segment(option == selected, label(option), brush) { onSelect(option) } }
    }
}

@Composable
private fun RowScope.Segment(
    selected: Boolean,
    text: String,
    brush: Brush,
    onSelect: () -> Unit,
) {
    val palette = GlassTheme.palette
    val progress by animateFloatAsState(if (selected) 1f else 0f, tween(GlassTokens.MOTION_SHORT), label = "segment")
    Box(
        Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
            .drawBehind { if (progress > 0f) drawRect(brush = brush, alpha = progress) }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = lerp(palette.textSecondary, palette.onAccent, progress),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/** Champagne track and dark knob when on; glass track and grey knob when off. */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = GlassTheme.palette
    val onBrush = remember(palette) { Brush.verticalGradient(listOf(palette.accentLight, palette.accentDeep)) }
    val progress by animateFloatAsState(if (checked) 1f else 0f, tween(GlassTokens.MOTION_SHORT), label = "switch")
    val direction = LocalLayoutDirection.current
    Box(
        modifier.size(UiDimensions.TOUCH_TARGET.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(GlassTokens.SWITCH_WIDTH.dp, GlassTokens.SWITCH_HEIGHT.dp)
                .clip(CircleShape)
                .background(palette.glassStrong)
                .drawBehind {
                    val radius = size.height / 2f
                    drawRoundRect(brush = onBrush, alpha = progress, cornerRadius = CornerRadius(radius))
                    val inset = GlassTokens.SWITCH_INSET.dp.toPx()
                    val thumb = GlassTokens.SWITCH_THUMB.dp.toPx()
                    val travel = size.width - thumb - inset * 2f
                    val start = if (direction == LayoutDirection.Rtl) {
                        size.width - inset - thumb - travel * progress
                    } else {
                        inset + travel * progress
                    }
                    val top = (size.height - thumb) / 2f
                    drawCircle(
                        color = lerp(palette.textTertiary, palette.onAccent, progress),
                        radius = thumb / 2f,
                        center = Offset(start + thumb / 2f, top + thumb / 2f),
                    )
                },
        )
    }
}
