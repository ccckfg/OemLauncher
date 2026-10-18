package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.GlassCard
import com.n2.oemlauncher.mobile.ui.components.GlassSegmented
import com.n2.oemlauncher.mobile.ui.components.GlassSettingRow
import com.n2.oemlauncher.mobile.ui.components.GlassSliderCard
import com.n2.oemlauncher.mobile.ui.components.GlassSwitch
import com.n2.oemlauncher.mobile.ui.components.GroupLabel
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconSize
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.VisualTier

@Composable
fun AppearanceScreen(state: CompanionState, vm: CompanionViewModel) {
    val settings = state.config.layout
    var strength by remember(settings.fisheyeStrength) { mutableFloatStateOf(settings.fisheyeStrength) }
    // The preview follows the slider while it moves, and the draft only on release.
    val previewState = remember(state, strength) {
        state.copy(
            snapshot = state.snapshot.copy(
                draft = state.draft?.copy(
                    current = ConfigEditor.updateLayout(state.config) { it.copy(fisheyeStrength = strength) },
                ),
            ),
        )
    }
    BoxWithConstraints {
        val wide = maxWidth >= UiDimensions.WIDE_BREAKPOINT.dp
        val previewEdge = (maxHeight.value - GlassTokens.PREVIEW_FOOTER).toInt()
            .coerceIn(GlassTokens.PREVIEW_MIN, GlassTokens.PREVIEW_MAX)
        val controls: @Composable (Modifier) -> Unit = { modifier ->
            LazyColumn(
                modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
            ) {
                item {
                    Choice(
                        title = stringResource(R.string.group_layout),
                        options = LayoutMode.entries, selected = settings.mode,
                        label = { stringResource(it.label()) },
                    ) { value -> vm.layout { it.copy(mode = value) } }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
                        GroupLabel(stringResource(R.string.group_icons))
                        GlassSegmented(
                            options = IconSize.entries, selected = settings.iconSize,
                            description = stringResource(R.string.icon_size),
                            label = { stringResource(it.label()) },
                        ) { value -> vm.layout { it.copy(iconSize = value) } }
                        GlassSegmented(
                            options = IconShape.entries, selected = settings.iconShape,
                            description = stringResource(R.string.icon_shape),
                            label = { stringResource(it.label()) },
                        ) { value -> vm.layout { it.copy(iconShape = value) } }
                        GlassSegmented(
                            options = COLUMN_CHOICES, selected = settings.gridColumns,
                            description = stringResource(R.string.grid_columns),
                            label = { stringResource(R.string.column_count, it) },
                        ) { value -> vm.layout { it.copy(gridColumns = value) } }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
                        GroupLabel(stringResource(R.string.visual_tier))
                        GlassSegmented(
                            options = VisualTier.entries, selected = settings.visualTier,
                            label = { stringResource(it.label()) },
                        ) { value -> vm.layout { it.copy(visualTier = value) } }
                        Text(
                            stringResource(R.string.tier_lite_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
                        GroupLabel(stringResource(R.string.icon_style))
                        GlassSegmented(
                            options = IconStyle.entries, selected = settings.iconStyle,
                            label = { stringResource(it.label()) },
                        ) { value -> vm.layout { it.copy(iconStyle = value) } }
                        Text(
                            stringResource(R.string.icon_style_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    GlassSliderCard(
                        label = stringResource(R.string.fisheye),
                        value = strength,
                        valueText = stringResource(R.string.percent, (strength * 100).toInt()),
                        valueRange = LauncherDefaults.FISHEYE_MIN..LauncherDefaults.FISHEYE_MAX,
                        description = stringResource(R.string.fisheye),
                        onValueChange = { strength = it },
                        onValueChangeFinished = { vm.layout { it.copy(fisheyeStrength = strength) } },
                    )
                }
                item {
                    GlassCard(padding = PaddingValues(horizontal = GlassTokens.CARD_PADDING.dp, vertical = 4.dp)) {
                        Column {
                            SwitchRow(R.string.show_labels, settings.showLabels) { value ->
                                vm.layout { it.copy(showLabels = value) }
                            }
                            SwitchRow(R.string.snap, settings.snapToCenter) { value ->
                                vm.layout { it.copy(snapToCenter = value) }
                            }
                            SwitchRow(R.string.haptics, settings.hapticsEnabled) { value ->
                                vm.layout { it.copy(hapticsEnabled = value) }
                            }
                            SwitchRow(R.string.reduce_motion, settings.reduceMotion) { value ->
                                vm.layout { it.copy(reduceMotion = value) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
            }
        }
        if (wide) {
            Row {
                controls(Modifier.weight(1f))
                Column(
                    Modifier.width(UiDimensions.PREVIEW_PANE.dp).padding(UiDimensions.GAP.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    WatchPreview(previewState, vm, maxEdgeDp = previewEdge)
                }
            }
        } else {
            Column {
                GlassCard(
                    modifier = Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
                    padding = PaddingValues(GlassTokens.CARD_PADDING.dp),
                ) {
                    WatchPreview(previewState, vm, diameterDp = GlassTokens.SMALL_PREVIEW)
                }
                controls(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun <T> Choice(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
        GroupLabel(title)
        GlassSegmented(options, selected, label = label, onSelect = onSelect)
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, change: (Boolean) -> Unit) {
    val description = stringResource(label)
    GlassSettingRow(stringResource(label)) {
        GlassSwitch(checked, change, description)
    }
}

private val COLUMN_CHOICES =
    (LauncherDefaults.GRID_COLUMNS_MIN..LauncherDefaults.GRID_COLUMNS_MAX).toList()
