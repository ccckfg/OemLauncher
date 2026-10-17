package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.design.GlassSpecs

/** The one solid champagne fill of a screen: the primary action (design section 1, rule 1). */
@Composable
fun ChampagneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = GlassTheme.palette
    val brush = remember(palette) { Brush.verticalGradient(listOf(palette.accentLight, palette.accentDeep)) }
    val shadow = Color.Black.copy(alpha = GlassSpecs.Phone.shadowAlpha)
    Box(
        modifier
            .height(GlassTokens.BUTTON_HEIGHT.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .shadow(12.dp, CircleShape, ambientColor = shadow, spotColor = shadow)
            .clip(CircleShape)
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.labelLarge, color = palette.onAccent,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** A glass capsule button: everything that is not the primary action. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .height(GlassTokens.BUTTON_HEIGHT.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .glassSurface(corner = 0.dp, capsule = true, strong = true, elevated = true)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** 44dp glass circle inside a 48dp touch target, so the target never shrinks below the spec. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier.size(UiDimensions.TOUCH_TARGET.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(ICON_BUTTON_DP.dp).alpha(if (enabled) 1f else DISABLED_ALPHA)
                .glassSurface(corner = 0.dp, capsule = true, strong = false),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(ICON_GLYPH_DP.dp),
            )
        }
    }
}

/** Empty states and errors are one sentence and one capsule button — never an illustration. */
@Composable
fun EmptyState(
    message: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(UiDimensions.PAGE_PADDING.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        ChampagneButton(action, onAction)
    }
}

private const val DISABLED_ALPHA = 0.4f
private const val ICON_BUTTON_DP = 44
private const val ICON_GLYPH_DP = 22
