package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Widgets
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.components.glassSurface
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.design.GlassSpecs

/** 2x2 entries: the layout editor is the primary one, so it is the only champagne tile. */
@Composable
fun EntryTiles(enabled: Boolean, navigate: (HomeDestination) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
            EntryTile(
                R.string.layout_editor, R.string.layout_editor_detail,
                Icons.AutoMirrored.Outlined.Sort, primary = true, enabled = enabled,
            ) { navigate(HomeDestination.EDITOR) }
            EntryTile(
                R.string.appearance, R.string.appearance_detail,
                Icons.Outlined.Widgets, enabled = enabled,
            ) { navigate(HomeDestination.APPEARANCE) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
            EntryTile(
                R.string.icons, R.string.icons_detail,
                Icons.Outlined.Palette, enabled = enabled,
            ) { navigate(HomeDestination.ICONS) }
            EntryTile(R.string.help, R.string.help_detail, Icons.AutoMirrored.Outlined.HelpOutline) {
                navigate(HomeDestination.HELP)
            }
        }
    }
}

@Composable
private fun RowScope.EntryTile(
    title: Int,
    detail: Int,
    icon: ImageVector,
    primary: Boolean = false,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    val palette = GlassTheme.palette
    val champagne = remember(palette) {
        Brush.verticalGradient(listOf(palette.accentLight, palette.accentDeep))
    }
    val shadow = Color.Black.copy(alpha = GlassSpecs.Phone.shadowAlpha)
    val ink = if (primary) palette.onAccent else MaterialTheme.colorScheme.onSurface
    val sub = if (primary) palette.onAccent.copy(alpha = SECONDARY_INK_ALPHA) else MaterialTheme.colorScheme.onSurfaceVariant
    val fill = if (primary) {
        Modifier.shadow(12.dp, RoundedCornerShape(GlassTokens.TILE_CORNER.dp), ambientColor = shadow, spotColor = shadow)
            .clip(RoundedCornerShape(GlassTokens.TILE_CORNER.dp))
            .background(champagne)
    } else {
        Modifier.glassSurface(GlassTokens.TILE_CORNER.dp, elevated = true)
    }
    Column(
        Modifier.weight(1f).height(GlassTokens.TILE_HEIGHT.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(fill)
            .clickable(enabled = enabled, onClick = action)
            .padding(GlassTokens.CARD_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(GlassTokens.TILE_ICON.dp).clip(CircleShape)
                .background(if (primary) palette.onAccent.copy(alpha = CHIP_FILL_ALPHA) else palette.glassStrong),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(TILE_GLYPH_DP.dp))
        }
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1)
        Text(
            stringResource(detail),
            style = MaterialTheme.typography.bodySmall,
            color = sub,
            maxLines = UiDimensions.DETAIL_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val TILE_GLYPH_DP = 22
private const val DISABLED_ALPHA = 0.4f
private const val CHIP_FILL_ALPHA = 0.1f
private const val SECONDARY_INK_ALPHA = 0.72f
