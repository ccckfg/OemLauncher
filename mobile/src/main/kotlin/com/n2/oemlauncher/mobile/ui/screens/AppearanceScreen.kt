package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.ChoiceRow
import com.n2.oemlauncher.mobile.ui.components.Section
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconSize
import com.n2.oemlauncher.shared.model.LayoutMode

@Composable
fun AppearanceScreen(state: CompanionState, vm: CompanionViewModel) {
    val settings = state.config.layout
    var strength by remember(settings.fisheyeStrength) { mutableFloatStateOf(settings.fisheyeStrength) }
    val previewState = remember(state, strength) {
        state.copy(snapshot = state.snapshot.copy(draft = state.draft?.copy(
            current = ConfigEditor.updateLayout(state.config) { it.copy(fisheyeStrength = strength) },
        )))
    }
    BoxWithConstraints {
        val wide = maxWidth >= UiDimensions.WIDE_BREAKPOINT.dp
        val previewEdge = (maxHeight.value - UiDimensions.PREVIEW_FOOTER).toInt()
            .coerceIn(UiDimensions.THUMBNAIL, UiDimensions.PREVIEW)
        val controls: @Composable (Modifier) -> Unit = { modifier ->
            LazyColumn(
                modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
            ) {
                item { Section(stringResource(R.string.layout_mode)) {
                    ChoiceRow(LayoutMode.entries, settings.mode, { it.label() }) { value -> vm.layout { it.copy(mode = value) } }
                } }
                item { Section(stringResource(R.string.icon_size)) {
                    ChoiceRow(IconSize.entries, settings.iconSize, { it.label() }) { value -> vm.layout { it.copy(iconSize = value) } }
                } }
                item { Section(stringResource(R.string.icon_shape)) {
                    ChoiceRow(IconShape.entries, settings.iconShape, { it.label() }) { value -> vm.layout { it.copy(iconShape = value) } }
                } }
                item { Section(stringResource(R.string.fisheye)) {
                    val sliderDescription = stringResource(R.string.fisheye)
                    Slider(
                        value = strength, onValueChange = { strength = it },
                        onValueChangeFinished = { vm.layout { it.copy(fisheyeStrength = strength) } },
                        valueRange = LauncherDefaults.FISHEYE_MIN..LauncherDefaults.FISHEYE_MAX,
                        modifier = Modifier.semantics { contentDescription = sliderDescription },
                    )
                } }
                item { Section(stringResource(R.string.grid_columns)) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
                    ) {
                        (LauncherDefaults.GRID_COLUMNS_MIN..LauncherDefaults.GRID_COLUMNS_MAX).forEach { count ->
                            androidx.compose.material3.FilterChip(
                                count == settings.gridColumns,
                                { vm.layout { it.copy(gridColumns = count) } },
                                label = { Text(stringResource(R.string.column_count, count)) },
                            )
                        }
                    }
                } }
                item { SettingSwitch(R.string.show_labels, settings.showLabels) { value -> vm.layout { it.copy(showLabels = value) } } }
                item { SettingSwitch(R.string.snap, settings.snapToCenter) { value -> vm.layout { it.copy(snapToCenter = value) } } }
                item { SettingSwitch(R.string.haptics, settings.hapticsEnabled) { value -> vm.layout { it.copy(hapticsEnabled = value) } } }
                item { SettingSwitch(R.string.reduce_motion, settings.reduceMotion) { value -> vm.layout { it.copy(reduceMotion = value) } } }
                item { Text(stringResource(R.string.saved_automatically), style = MaterialTheme.typography.bodySmall) }
                item { androidx.compose.foundation.layout.Spacer(Modifier.padding(UiDimensions.GAP.dp)) }
            }
        }
        if (wide) Row {
            controls(Modifier.weight(1f))
            Column(Modifier.width(UiDimensions.PREVIEW_PANE.dp).verticalScroll(rememberScrollState())) {
                WatchPreview(previewState, vm.container.icons, maxEdgeDp = previewEdge)
            }
        } else Column {
            WatchPreview(previewState, vm.container.icons, thumbnail = true)
            controls(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SettingSwitch(label: Int, checked: Boolean, change: (Boolean) -> Unit) {
    val description = stringResource(label)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked, change, modifier = Modifier.semantics { contentDescription = description })
    }
}
