package com.n2.oemlauncher.wear.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hive
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.theme.OemColors

/** Quick switch between the three layouts; tapping outside the capsule or Back closes it. */
@Composable
fun LayoutSwitcher(current: LayoutMode, onSelect: (LayoutMode) -> Unit, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize()
            .background(OemColors.Scrim.copy(alpha = WearUiTokens.SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .padding(WearUiTokens.SWITCHER_PADDING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(WearUiTokens.SWITCHER_GAP_DP.dp),
        ) {
            LayoutMode.entries.forEach { mode ->
                val icon = layoutIcon(mode)
                val label = layoutName(mode)
                val size = Modifier.size(WearUiTokens.SWITCHER_BUTTON_DP.dp)
                if (mode == current) {
                    FilledIconButton(onClick = { onSelect(mode) }, modifier = size) { Icon(icon, label) }
                } else {
                    IconButton(onClick = { onSelect(mode) }, modifier = size) { Icon(icon, label) }
                }
            }
        }
    }
}

fun layoutIcon(mode: LayoutMode): ImageVector = when (mode) {
    LayoutMode.HONEYCOMB -> Icons.Rounded.Hive
    LayoutMode.GRID -> Icons.Rounded.GridView
    LayoutMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
}

@Composable
fun layoutName(mode: LayoutMode): String = stringResource(
    when (mode) {
        LayoutMode.HONEYCOMB -> R.string.layout_honeycomb
        LayoutMode.GRID -> R.string.layout_grid
        LayoutMode.LIST -> R.string.layout_list
    },
)
