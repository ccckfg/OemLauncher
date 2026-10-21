package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme

/** A glass card over the page, centered; the page dims and blurs behind it. */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) = ShowOverlay(sheet = false, onDismissRequest, content)

/** A floating glass sheet from the bottom edge; swipe it down or tap outside to close it. */
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) = ShowOverlay(sheet = true, onDismissRequest, content)

/** A row of a sheet: an action with an icon, as tall as a list row. */
@Composable
fun SheetAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().height(GlassTokens.ROW_HEIGHT.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = GlassTokens.CARD_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        Icon(
            icon, null,
            tint = tint ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(SHEET_ICON_DP.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** The sheet's subject, small and quiet above its actions. */
@Composable
fun SheetTitle(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(horizontal = GlassTokens.CARD_PADDING.dp, vertical = TITLE_PADDING_DP.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun SheetHandle() {
    Box(Modifier.fillMaxWidth().height(HANDLE_AREA_DP.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(HANDLE_WIDTH_DP.dp, HANDLE_HEIGHT_DP.dp)
                .background(GlassTheme.palette.textSecondary.copy(alpha = HANDLE_ALPHA), RoundedCornerShape(50)),
        )
    }
}

private const val HANDLE_AREA_DP = 24
private const val HANDLE_WIDTH_DP = 36
private const val HANDLE_HEIGHT_DP = 5
private const val HANDLE_ALPHA = 0.5f
private const val SHEET_ICON_DP = 22
private const val TITLE_PADDING_DP = 6
private const val DISABLED_ALPHA = 0.38f
