package com.n2.oemlauncher.wear.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.wear.BuildConfig
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.AppGlyph
import com.n2.oemlauncher.wear.ui.LauncherUiState
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.ui.RotaryTicksScroll
import com.n2.oemlauncher.wear.ui.layoutIcon
import com.n2.oemlauncher.wear.ui.layoutName
import com.n2.oemlauncher.wear.ui.theme.oemBackdrop
import kotlin.math.roundToInt

/**
 * Grouped settings: choices open a radio page, on/off values are switches and numbers are
 * steppers. The Done edge button stays at the bottom; a swipe to the right also leaves.
 */
@Composable
fun SettingsScreen(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    onChoice: (SettingChoice) -> Unit,
    onDone: () -> Unit,
) {
    val config = state.config
    val layout = config.layout
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    var confirmReset by remember { mutableStateOf(false) }
    val updateLayout: ((LayoutSettings) -> LayoutSettings) -> Unit = { change ->
        viewModel.updateConfig { ConfigEditor.updateLayout(it, change) }
    }
    val labels = remember(state.entries) { state.entries.associate { it.key to it.label } }
    val fisheyeSteps = WearUiTokens.FISHEYE_STEPS
    RotaryTicksScroll(listState, state.rotaryTicks, layout.hapticsEnabled)

    ScreenScaffold(
        scrollState = listState,
        modifier = Modifier.oemBackdrop(),
        edgeButton = {
            EdgeButton(onClick = onDone, buttonSize = EdgeButtonSize.Small) {
                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.done))
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text(stringResource(R.string.settings)) }
            }
            item { SectionHeader(stringResource(R.string.section_appearance), spec) }
            item {
                ActionRow(vectorIcon(layoutIcon(layout.mode)), stringResource(R.string.label_layout), layoutName(layout.mode), spec) {
                    onChoice(SettingChoice.LAYOUT)
                }
            }
            item {
                ActionRow(vectorIcon(Icons.Rounded.PhotoSizeSelectLarge), stringResource(R.string.label_icon_size), sizeName(layout.iconSize), spec) {
                    onChoice(SettingChoice.ICON_SIZE)
                }
            }
            item {
                ActionRow(vectorIcon(Icons.Rounded.Interests), stringResource(R.string.label_icon_shape), shapeName(layout.iconShape), spec) {
                    onChoice(SettingChoice.ICON_SHAPE)
                }
            }
            item {
                ActionRow(vectorIcon(Icons.AutoMirrored.Rounded.Sort), stringResource(R.string.label_sort), sortName(config.sortMode), spec) {
                    onChoice(SettingChoice.SORT)
                }
            }
            item {
                ToggleRow(Icons.AutoMirrored.Rounded.Label, stringResource(R.string.label_show_names), layout.showLabels, spec) { on ->
                    updateLayout { it.copy(showLabels = on) }
                }
            }
            item {
                val value = (layout.fisheyeStrength * fisheyeSteps).roundToInt()
                SliderRow(
                    label = stringResource(R.string.label_fisheye),
                    valueText = stringResource(R.string.percent_value, value * 100 / fisheyeSteps),
                    value = value,
                    steps = 0..fisheyeSteps,
                    spec = spec,
                ) { step -> updateLayout { it.copy(fisheyeStrength = step.toFloat() / fisheyeSteps) } }
            }
            item {
                SliderRow(
                    label = stringResource(R.string.label_grid_columns),
                    valueText = layout.gridColumns.toString(),
                    value = layout.gridColumns,
                    steps = LauncherDefaults.GRID_COLUMNS_MIN..LauncherDefaults.GRID_COLUMNS_MAX,
                    spec = spec,
                ) { columns -> updateLayout { it.copy(gridColumns = columns) } }
            }

            item { SectionHeader(stringResource(R.string.section_interaction), spec) }
            item {
                ToggleRow(Icons.Rounded.CenterFocusStrong, stringResource(R.string.label_snap), layout.snapToCenter, spec) { on ->
                    updateLayout { it.copy(snapToCenter = on) }
                }
            }
            item {
                ToggleRow(Icons.Rounded.Vibration, stringResource(R.string.label_haptics), layout.hapticsEnabled, spec) { on ->
                    updateLayout { it.copy(hapticsEnabled = on) }
                }
            }
            item {
                ToggleRow(Icons.Rounded.MotionPhotosOff, stringResource(R.string.label_reduce_motion), layout.reduceMotion, spec) { on ->
                    updateLayout { it.copy(reduceMotion = on) }
                }
            }

            if (config.favorites.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.section_favorites), spec) }
                items(config.favorites, key = { "favorite-$it" }) { key ->
                    ActionRow(
                        icon = { AppGlyph(state.icons[key], Modifier.size(ButtonDefaults.IconSize)) },
                        label = labels[key] ?: key.substringBefore('/'),
                        value = stringResource(R.string.favorite_move_down),
                        spec = spec,
                    ) { viewModel.moveFavorite(key, 1) }
                }
            }
            if (state.hiddenEntries.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.hidden_apps), spec) }
                items(state.hiddenEntries, key = { "hidden-${it.key}" }) { entry ->
                    ActionRow(
                        icon = { AppGlyph(state.icons[entry.key], Modifier.size(ButtonDefaults.IconSize)) },
                        label = entry.label,
                        value = stringResource(R.string.unhide_hint),
                        spec = spec,
                    ) { viewModel.unhide(entry.key) }
                }
            }

            item { SectionHeader(stringResource(R.string.section_sync), spec) }
            item { SyncStatusCard(state, spec) }

            item { SectionHeader(stringResource(R.string.section_about), spec) }
            if (BuildConfig.DEBUG) {
                item {
                    ToggleRow(Icons.Rounded.Science, stringResource(R.string.label_demo_data), state.demoEnabled, spec) {
                        viewModel.toggleDemo()
                    }
                }
            }
            item {
                ActionRow(
                    icon = vectorIcon(Icons.Rounded.RestartAlt),
                    label = stringResource(R.string.restore_defaults),
                    value = null,
                    spec = spec,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error,
                    ),
                ) { confirmReset = true }
            }
            item {
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    Modifier.fillMaxWidth().transformedHeight(this, spec).padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    AlertDialog(
        visible = confirmReset,
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.restore_defaults_title)) },
        text = { Text(stringResource(R.string.restore_defaults_message)) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(onClick = {
                viewModel.updateConfig { LauncherConfig() }
                confirmReset = false
            })
        },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = { confirmReset = false }) },
    )
}
