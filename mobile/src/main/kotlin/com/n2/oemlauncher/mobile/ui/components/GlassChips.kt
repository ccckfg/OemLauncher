package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions

/**
 * Connection state and similar facts: a glass capsule, with a colored dot in front when the
 * state is good. The dot never carries the meaning alone — the text says it too.
 */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, dot: Color? = null) {
    ChipBody(text, icon = null, enabled = true, modifier = modifier) {
        if (dot != null) Box(Modifier.size(GlassTokens.STATUS_DOT.dp).background(dot, CircleShape))
    }
}

/** A chip that acts, like "sync now"; the touch target grows to 48dp around the 34dp capsule. */
@Composable
fun ActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier.height(UiDimensions.TOUCH_TARGET.dp).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ChipBody(text, icon, enabled) {}
    }
}

@Composable
private fun ChipBody(
    text: String,
    icon: ImageVector?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier.height(GlassTokens.CHIP_HEIGHT.dp).alpha(if (enabled) 1f else DISABLED_ALPHA)
            .glassSurface(corner = 0.dp, capsule = true, strong = true)
            .padding(horizontal = GlassTokens.CHIP_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
    ) {
        leading()
        if (icon != null) {
            Icon(
                icon, null, tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(CHIP_ICON_DP.dp),
            )
        }
        Text(
            text, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val DISABLED_ALPHA = 0.4f
private const val CHIP_ICON_DP = 16
