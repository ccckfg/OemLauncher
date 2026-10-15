package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconSize
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.wear.BuildConfig
import com.n2.oemlauncher.wear.R

@Composable
fun SettingsScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val config = state.config
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 34.dp)) {
        item { Text(stringResource(R.string.settings), color = Color.White, fontSize = 16.sp) }
        item { SettingRow(stringResource(R.string.layout_mode, layoutLabel(config.layout.mode))) { viewModel.setLayout(nextMode(config.layout.mode)) } }
        item { SettingRow(stringResource(R.string.icon_size, sizeLabel(config.layout.iconSize))) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(iconSize = nextSize(l.iconSize)) } } } }
        item { SettingRow(stringResource(R.string.icon_shape, shapeLabel(config.layout.iconShape))) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(iconShape = nextShape(l.iconShape)) } } } }
        item { SettingRow(stringResource(R.string.fisheye, (config.layout.fisheyeStrength * 100).toInt())) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(fisheyeStrength = (l.fisheyeStrength + 0.1f).let { if (it > LauncherDefaults.FISHEYE_MAX) 0f else it }) } } } }
        item { SettingRow(stringResource(R.string.grid_columns, config.layout.gridColumns)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(gridColumns = if (l.gridColumns >= LauncherDefaults.GRID_COLUMNS_MAX) LauncherDefaults.GRID_COLUMNS_MIN else l.gridColumns + 1) } } } }
        item { SettingRow(stringResource(R.string.show_labels, boolLabel(config.layout.showLabels))) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(showLabels = !l.showLabels) } } } }
        item { SettingRow(stringResource(R.string.snap_center, boolLabel(config.layout.snapToCenter))) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(snapToCenter = !l.snapToCenter) } } } }
        item { SettingRow(stringResource(R.string.haptics, boolLabel(config.layout.hapticsEnabled))) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(hapticsEnabled = !l.hapticsEnabled) } } } }
        item { SettingRow(stringResource(R.string.reduce_motion, boolLabel(config.layout.reduceMotion))) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(reduceMotion = !l.reduceMotion) } } } }
        item { SettingRow(stringResource(R.string.sort_mode, sortLabel(config.sortMode))) { viewModel.setSortMode(nextSort(config.sortMode)) } }
        if (config.favorites.isNotEmpty()) {
            item { Text(stringResource(R.string.favorite_order), color = Color.LightGray, fontSize = 14.sp) }
            items(config.favorites, key = { it }) { key ->
                SettingRow(stringResource(R.string.favorite_entry, key.substringAfterLast('/'))) { viewModel.moveFavorite(key, 1) }
            }
        }
        if (config.hidden.isNotEmpty()) {
            item { Text(stringResource(R.string.hidden_apps), color = Color.LightGray, fontSize = 14.sp) }
            items(config.hidden, key = { it }) { key ->
                SettingRow(stringResource(R.string.unhide_app, key.substringAfterLast('/'))) {
                    viewModel.updateConfig { ConfigEditor.setHidden(it, key, false) }
                }
            }
        }
        item { SettingRow(stringResource(R.string.restore_defaults)) { viewModel.updateConfig { com.n2.oemlauncher.shared.model.LauncherConfig() } } }
        if (BuildConfig.DEBUG) item { SettingRow(stringResource(R.string.demo_data, boolLabel(state.demoEnabled))) { viewModel.toggleDemo() } }
        item { Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), Modifier.padding(top = 16.dp), color = Color.LightGray) }
        item { Text(stringResource(R.string.sync_status, syncStatusLabel(state)), Modifier.padding(top = 8.dp), color = Color.LightGray, fontSize = 13.sp) }
        item {
            val lastSync = state.syncLastText
            Text(
                if (lastSync == null) stringResource(R.string.sync_time_never) else stringResource(R.string.sync_time, lastSync),
                color = Color.LightGray,
                fontSize = 13.sp,
            )
        }
        item { Text(stringResource(R.string.sync_result, syncResultLabel(state)), color = Color.LightGray, fontSize = 13.sp) }
        item { SettingRow(stringResource(R.string.done)) { viewModel.openSettings(false) } }
    }
}

@Composable
private fun SettingRow(label: String, onClick: () -> Unit) =
    Text(label, Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp), color = Color.White, fontSize = 14.sp)

private fun nextMode(mode: LayoutMode) = when (mode) { LayoutMode.HONEYCOMB -> LayoutMode.GRID; LayoutMode.GRID -> LayoutMode.LIST; LayoutMode.LIST -> LayoutMode.HONEYCOMB }
private fun nextSize(size: IconSize) = when (size) { IconSize.SMALL -> IconSize.MEDIUM; IconSize.MEDIUM -> IconSize.LARGE; IconSize.LARGE -> IconSize.SMALL }
private fun nextShape(shape: IconShape) = when (shape) { IconShape.CIRCLE -> IconShape.SQUIRCLE; IconShape.SQUIRCLE -> IconShape.ORIGINAL; IconShape.ORIGINAL -> IconShape.CIRCLE }
private fun nextSort(mode: SortMode) = when (mode) { SortMode.CUSTOM -> SortMode.ALPHABETICAL; SortMode.ALPHABETICAL -> SortMode.MOST_USED; SortMode.MOST_USED -> SortMode.RECENTLY_USED; SortMode.RECENTLY_USED -> SortMode.CUSTOM }

@Composable private fun boolLabel(value: Boolean) = stringResource(if (value) R.string.value_on else R.string.value_off)
@Composable private fun layoutLabel(value: LayoutMode) = stringResource(when (value) { LayoutMode.HONEYCOMB -> R.string.layout_honeycomb; LayoutMode.GRID -> R.string.layout_grid; LayoutMode.LIST -> R.string.layout_list })
@Composable private fun sizeLabel(value: IconSize) = stringResource(when (value) { IconSize.SMALL -> R.string.size_small; IconSize.MEDIUM -> R.string.size_medium; IconSize.LARGE -> R.string.size_large })
@Composable private fun shapeLabel(value: IconShape) = stringResource(when (value) { IconShape.CIRCLE -> R.string.shape_circle; IconShape.SQUIRCLE -> R.string.shape_squircle; IconShape.ORIGINAL -> R.string.shape_original })
@Composable private fun sortLabel(value: SortMode) = stringResource(when (value) { SortMode.CUSTOM -> R.string.sort_custom; SortMode.ALPHABETICAL -> R.string.sort_alphabetical; SortMode.MOST_USED -> R.string.sort_most_used; SortMode.RECENTLY_USED -> R.string.sort_recent })

@Composable private fun syncStatusLabel(state: LauncherUiState) = stringResource(
    when (state.syncConnected) {
        null -> R.string.sync_status_never
        true -> R.string.sync_status_connected
        false -> R.string.sync_status_disconnected
    },
)

@Composable private fun syncResultLabel(state: LauncherUiState) = when (state.syncResult) {
    ApplyResult.NONE -> stringResource(R.string.sync_result_none)
    ApplyResult.APPLIED -> stringResource(R.string.sync_result_applied)
    ApplyResult.APPLIED_MISSING_ICONS -> stringResource(R.string.sync_result_missing_icons, state.syncMissingIcons)
    ApplyResult.REJECTED_STALE -> stringResource(R.string.sync_result_rejected_stale)
    ApplyResult.REJECTED_INVALID -> stringResource(R.string.sync_result_rejected_invalid)
    ApplyResult.REJECTED_SCHEMA -> stringResource(R.string.sync_result_rejected_schema)
}
