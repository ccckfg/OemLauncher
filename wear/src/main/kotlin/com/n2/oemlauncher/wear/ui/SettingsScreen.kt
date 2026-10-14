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
        item { SettingRow(stringResource(R.string.layout_mode, config.layout.mode.name)) { viewModel.setLayout(nextMode(config.layout.mode)) } }
        item { SettingRow(stringResource(R.string.icon_size, config.layout.iconSize.name)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(iconSize = nextSize(l.iconSize)) } } } }
        item { SettingRow(stringResource(R.string.icon_shape, config.layout.iconShape.name)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(iconShape = nextShape(l.iconShape)) } } } }
        item { SettingRow(stringResource(R.string.fisheye, (config.layout.fisheyeStrength * 100).toInt())) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(fisheyeStrength = (l.fisheyeStrength + 0.1f).let { if (it > LauncherDefaults.FISHEYE_MAX) 0f else it }) } } } }
        item { SettingRow(stringResource(R.string.grid_columns, config.layout.gridColumns)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(gridColumns = if (l.gridColumns >= LauncherDefaults.GRID_COLUMNS_MAX) LauncherDefaults.GRID_COLUMNS_MIN else l.gridColumns + 1) } } } }
        item { SettingRow(stringResource(R.string.show_labels, config.layout.showLabels)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(showLabels = !l.showLabels) } } } }
        item { SettingRow(stringResource(R.string.snap_center, config.layout.snapToCenter)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(snapToCenter = !l.snapToCenter) } } } }
        item { SettingRow(stringResource(R.string.haptics, config.layout.hapticsEnabled)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(hapticsEnabled = !l.hapticsEnabled) } } } }
        item { SettingRow(stringResource(R.string.reduce_motion, config.layout.reduceMotion)) { viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(reduceMotion = !l.reduceMotion) } } } }
        item { SettingRow(stringResource(R.string.sort_mode, config.sortMode.name)) { viewModel.setSortMode(nextSort(config.sortMode)) } }
        if (config.hidden.isNotEmpty()) {
            item { Text(stringResource(R.string.hidden_apps), color = Color.LightGray, fontSize = 14.sp) }
            items(config.hidden, key = { it }) { key ->
                SettingRow(stringResource(R.string.unhide_app, key.substringAfterLast('/'))) {
                    viewModel.updateConfig { ConfigEditor.setHidden(it, key, false) }
                }
            }
        }
        item { SettingRow(stringResource(R.string.restore_defaults)) { viewModel.updateConfig { com.n2.oemlauncher.shared.model.LauncherConfig() } } }
        if (BuildConfig.DEBUG) item { SettingRow(stringResource(R.string.demo_data, state.demoEnabled)) { viewModel.toggleDemo() } }
        item { Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), Modifier.padding(top = 16.dp), color = Color.LightGray) }
        item { Text(stringResource(R.string.sync_time, state.lastSyncMillis), color = Color.LightGray) }
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
