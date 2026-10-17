package com.n2.oemlauncher.wear.ui

import android.graphics.Bitmap
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.wear.config.WearUiTokens

/**
 * Icon-and-name list whose rows follow the left edge of the circle and shrink towards the top
 * and bottom, with the system scroll indicator on the right.
 */
@Composable
fun ListView(state: LauncherUiState, viewModel: LauncherViewModel, callbacks: HomeCallbacks) {
    val layout = state.config.layout
    val listState = rememberTransformingLazyColumnState()
    RotaryTicksScroll(listState, state.rotaryTicks, layout.hapticsEnabled)
    ScreenScaffold(scrollState = listState) { _ ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val arcPx = with(LocalDensity.current) { maxWidth.toPx() } / 2f * WearUiTokens.LIST_ARC_FACTOR
            TransformingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = WearUiTokens.LIST_START_PADDING_DP.dp,
                    end = WearUiTokens.LIST_END_PADDING_DP.dp,
                    top = WearUiTokens.LIST_VERTICAL_PADDING_DP.dp,
                    bottom = WearUiTokens.LIST_VERTICAL_PADDING_DP.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(WearUiTokens.LIST_ROW_SPACING_DP.dp),
            ) {
                item(key = SETTINGS_KEY) { HomeSettingsButton(callbacks, Modifier.fillMaxWidth().roundEdge(this)) }
                items(state.entries, key = { it.key }) { entry ->
                    ListRow(
                        entry = entry,
                        icon = state.icons[entry.key],
                        favorite = entry.key in state.config.favorites,
                        showLabel = layout.showLabels,
                        modifier = Modifier.fillMaxWidth().roundEdge(this, arcPx),
                        onClick = { viewModel.launch(entry) },
                        onLongClick = { callbacks.openMenu(entry.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ListRow(
    entry: CatalogEntry,
    icon: Bitmap?,
    favorite: Boolean,
    showLabel: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier
            .heightIn(min = WearUiTokens.LIST_ROW_MIN_HEIGHT_DP.dp)
            .clip(CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = entry.label })
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppGlyph(icon, Modifier.size(WearUiTokens.LIST_ICON_DP.dp), favorite = favorite)
        if (showLabel) {
            Text(
                entry.label,
                Modifier.padding(start = WearUiTokens.LIST_LABEL_GAP_DP.dp),
                color = com.n2.oemlauncher.wear.ui.theme.OemColors.TextPrimary,
                fontSize = WearUiTokens.LIST_LABEL_SP.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
