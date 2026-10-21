package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.GlassCard
import com.n2.oemlauncher.mobile.ui.components.GlassSegmented
import com.n2.oemlauncher.mobile.ui.components.GlassSettingRow
import com.n2.oemlauncher.mobile.ui.components.GlassSwitch
import com.n2.oemlauncher.mobile.ui.components.GroupLabel
import com.n2.oemlauncher.mobile.ui.components.LabeledSlider
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconSize
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.VisualTier

/**
 * Appearance settings in three glass cards: icons, the watch, behavior. Settings that do nothing
 * in the current layout (columns outside the grid, fisheye outside the honeycomb) stay hidden.
 */
@Composable
fun AppearanceControls(
    settings: LayoutSettings,
    strength: Float,
    onStrength: (Float) -> Unit,
    vm: CompanionViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        contentPadding = PaddingValues(top = UiDimensions.GAP.dp, bottom = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP.dp),
    ) {
        item {
            Section(stringResource(R.string.group_icons)) {
                Choice(R.string.icon_size, IconSize.entries, settings.iconSize, { stringResource(it.label()) }) { value ->
                    vm.layout { it.copy(iconSize = value) }
                }
                Choice(R.string.icon_shape, IconShape.entries, settings.iconShape, { stringResource(it.label()) }) { value ->
                    vm.layout { it.copy(iconShape = value) }
                }
                if (settings.mode == LayoutMode.GRID) {
                    Choice(
                        R.string.grid_columns, COLUMN_CHOICES, settings.gridColumns,
                        { stringResource(R.string.column_count, it) },
                    ) { value -> vm.layout { it.copy(gridColumns = value) } }
                }
                Choice(
                    R.string.icon_style, IconStyle.entries, settings.iconStyle, { stringResource(it.label()) },
                    note = R.string.icon_style_note.takeIf { settings.iconStyle == IconStyle.THEMED },
                ) { value -> vm.layout { it.copy(iconStyle = value) } }
            }
        }
        item {
            Section(stringResource(R.string.group_watch)) {
                Choice(
                    R.string.visual_tier, VisualTier.entries, settings.visualTier, { stringResource(it.label()) },
                    note = R.string.tier_lite_note,
                ) { value -> vm.layout { it.copy(visualTier = value) } }
                if (settings.mode == LayoutMode.HONEYCOMB) {
                    LabeledSlider(
                        label = stringResource(R.string.fisheye),
                        value = strength,
                        valueText = stringResource(R.string.percent, (strength * 100).toInt()),
                        valueRange = LauncherDefaults.FISHEYE_MIN..LauncherDefaults.FISHEYE_MAX,
                        description = stringResource(R.string.fisheye),
                        onValueChange = onStrength,
                        onValueChangeFinished = { vm.layout { it.copy(fisheyeStrength = strength) } },
                        modifier = Modifier.padding(top = BLOCK_PADDING.dp),
                    )
                }
            }
        }
        item {
            Section(stringResource(R.string.group_behavior)) {
                SwitchRow(R.string.show_labels, settings.showLabels) { value -> vm.layout { it.copy(showLabels = value) } }
                SwitchRow(R.string.snap, settings.snapToCenter) { value -> vm.layout { it.copy(snapToCenter = value) } }
                SwitchRow(R.string.haptics, settings.hapticsEnabled) { value ->
                    vm.layout { it.copy(hapticsEnabled = value) }
                }
                SwitchRow(R.string.reduce_motion, settings.reduceMotion) { value ->
                    vm.layout { it.copy(reduceMotion = value) }
                }
            }
        }
    }
}

/** A champagne group header over one glass card, like the watch's settings pages. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
        GroupLabel(title)
        GlassCard(padding = PaddingValues(horizontal = GlassTokens.CARD_PADDING.dp, vertical = CARD_VERTICAL.dp)) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

/** One choice inside a card: its name, the segmented control and an optional note under it. */
@Composable
private fun <T> Choice(
    title: Int,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    note: Int? = null,
    onSelect: (T) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = BLOCK_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
    ) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        GlassSegmented(
            options, selected, description = stringResource(title), strong = true,
            label = label, onSelect = onSelect,
        )
        if (note != null) {
            Text(
                stringResource(note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, change: (Boolean) -> Unit) {
    val description = stringResource(label)
    GlassSettingRow(description) { GlassSwitch(checked, change, description) }
}

private val COLUMN_CHOICES =
    (LauncherDefaults.GRID_COLUMNS_MIN..LauncherDefaults.GRID_COLUMNS_MAX).toList()
private const val SECTION_GAP = 20
private const val CARD_VERTICAL = 6
private const val BLOCK_PADDING = 10
