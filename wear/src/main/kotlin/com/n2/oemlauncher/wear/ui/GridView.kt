package com.n2.oemlauncher.wear.ui

import android.graphics.Bitmap
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.wear.config.WearUiTokens

/**
 * Grid of round icons. Rows shrink and fade near the top and bottom edges so nothing is ever
 * cut by the circle; a short last row stays centered.
 */
@Composable
fun GridView(state: LauncherUiState, viewModel: LauncherViewModel, callbacks: HomeCallbacks) {
    val layout = state.config.layout
    val columns = layout.gridColumns.coerceAtLeast(1)
    val rows = remember(state.entries, columns) { state.entries.chunked(columns) }
    val listState = rememberTransformingLazyColumnState()
    RotaryTicksScroll(listState, state.rotaryTicks, layout.hapticsEnabled)
    ScreenScaffold(scrollState = listState) { _ ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val spacing = WearUiTokens.GRID_SPACING_DP.dp
            val horizontal = WearUiTokens.GRID_HORIZONTAL_PADDING_DP.dp
            val cellWidth = (maxWidth - horizontal * 2 - spacing * (columns - 1)) / columns
            // Icons must stay perfectly round: cap them to the cell width the column count allows.
            val iconSize = minOf(
                HoneycombDefaults.iconDiameterDp(layout.iconSize).dp,
                cellWidth - WearUiTokens.GRID_CELL_PADDING_DP.dp * 2,
            )
            TransformingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontal, vertical = WearUiTokens.GRID_VERTICAL_PADDING_DP.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                item(key = SETTINGS_KEY) {
                    HomeSettingsButton(callbacks, Modifier.fillMaxWidth().roundEdge(this), layout.visualTier)
                }
                items(rows, key = { row -> row.first().key }) { row ->
                    Row(
                        Modifier.fillMaxWidth().roundEdge(this),
                        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { entry ->
                            GridCell(
                                entry = entry,
                                icon = state.icons[entry.key],
                                favorite = entry.key in state.config.favorites,
                                showLabel = layout.showLabels,
                                iconShape = layout.iconShape,
                                iconSize = iconSize,
                                modifier = Modifier.width(cellWidth),
                                onClick = { viewModel.launch(entry) },
                                onLongClick = { callbacks.openMenu(entry.key) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    entry: CatalogEntry,
    icon: Bitmap?,
    favorite: Boolean,
    showLabel: Boolean,
    iconShape: com.n2.oemlauncher.shared.model.IconShape,
    iconSize: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = entry.label })
            .padding(WearUiTokens.GRID_CELL_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppGlyph(icon, Modifier.size(iconSize), iconShape, favorite = favorite)
        if (showLabel) {
            Text(
                entry.label,
                Modifier.fillMaxWidth().padding(top = 3.dp),
                color = com.n2.oemlauncher.wear.ui.theme.OemColors.TextPrimary,
                fontSize = WearUiTokens.GRID_LABEL_SP.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal const val SETTINGS_KEY = "settings"
