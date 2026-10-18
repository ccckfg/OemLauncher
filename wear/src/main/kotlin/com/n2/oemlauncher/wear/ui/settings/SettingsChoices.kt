package com.n2.oemlauncher.wear.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconSize
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.LauncherUiState
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.ui.RotaryTicksScroll
import com.n2.oemlauncher.wear.ui.layoutName
import com.n2.oemlauncher.wear.ui.theme.oemBackdrop

/** Settings with a few fixed options get their own page with one radio button per option. */
enum class SettingChoice { LAYOUT, ICON_SIZE, ICON_SHAPE, VISUAL_TIER, ICON_STYLE, SORT }

private class ChoiceOption(val label: String, val selected: Boolean, val apply: () -> Unit)

@Composable
fun ChoiceScreen(choice: SettingChoice, state: LauncherUiState, viewModel: LauncherViewModel, onDone: () -> Unit) {
    val config = state.config
    val layout = config.layout
    val options = when (choice) {
        SettingChoice.LAYOUT -> LayoutMode.entries.map { mode ->
            ChoiceOption(layoutName(mode), mode == layout.mode) { viewModel.setLayout(mode) }
        }
        SettingChoice.ICON_SIZE -> IconSize.entries.map { size ->
            ChoiceOption(sizeName(size), size == layout.iconSize) {
                viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(iconSize = size) } }
            }
        }
        SettingChoice.ICON_SHAPE -> IconShape.entries.map { shape ->
            ChoiceOption(shapeName(shape), shape == layout.iconShape) {
                viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(iconShape = shape) } }
            }
        }
        SettingChoice.VISUAL_TIER -> VisualTier.entries.map { tier ->
            ChoiceOption(visualTierName(tier), tier == layout.visualTier) {
                viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(visualTier = tier) } }
            }
        }
        SettingChoice.ICON_STYLE -> IconStyle.entries.map { style ->
            ChoiceOption(iconStyleName(style), style == layout.iconStyle) {
                viewModel.updateConfig { ConfigEditor.updateLayout(it) { l -> l.copy(iconStyle = style) } }
            }
        }
        SettingChoice.SORT -> SortMode.entries.map { mode ->
            ChoiceOption(sortName(mode), mode == config.sortMode) { viewModel.setSortMode(mode) }
        }
    }
    val title = stringResource(choiceTitle(choice))
    val selected = options.indexOfFirst { it.selected }
    // Item 0 is the header, so option i is item i + 1.
    val anchor = if (selected >= WearUiTokens.CHOICE_VISIBLE_OPTIONS) selected + 1 else 0
    val listState = rememberTransformingLazyColumnState(initialAnchorItemIndex = anchor)
    val spec = rememberTransformationSpec()
    RotaryTicksScroll(listState, state.rotaryTicks, layout.hapticsEnabled)
    ScreenScaffold(scrollState = listState, modifier = Modifier.oemBackdrop(layout.visualTier)) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text(title) }
            }
            items(options) { option ->
                RadioButton(
                    selected = option.selected,
                    onSelect = {
                        option.apply()
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    label = { Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

private fun choiceTitle(choice: SettingChoice): Int = when (choice) {
    SettingChoice.LAYOUT -> R.string.label_layout
    SettingChoice.ICON_SIZE -> R.string.label_icon_size
    SettingChoice.ICON_SHAPE -> R.string.label_icon_shape
    SettingChoice.VISUAL_TIER -> R.string.label_visual_tier
    SettingChoice.ICON_STYLE -> R.string.label_icon_style
    SettingChoice.SORT -> R.string.label_sort
}

@Composable
fun sizeName(size: IconSize): String = stringResource(
    when (size) {
        IconSize.SMALL -> R.string.size_small
        IconSize.MEDIUM -> R.string.size_medium
        IconSize.LARGE -> R.string.size_large
    },
)

@Composable
fun shapeName(shape: IconShape): String = stringResource(
    when (shape) {
        IconShape.CIRCLE -> R.string.shape_circle
        IconShape.SQUIRCLE -> R.string.shape_squircle
        IconShape.ORIGINAL -> R.string.shape_original
    },
)

@Composable
fun visualTierName(tier: VisualTier): String = stringResource(
    when (tier) {
        VisualTier.REFINED -> R.string.visual_refined
        VisualTier.LITE -> R.string.visual_lite
    },
)

@Composable
fun iconStyleName(style: IconStyle): String = stringResource(
    when (style) {
        IconStyle.ORIGINAL -> R.string.icon_style_original
        IconStyle.THEMED -> R.string.icon_style_themed
    },
)

@Composable
fun sortName(mode: SortMode): String = stringResource(
    when (mode) {
        SortMode.CUSTOM -> R.string.sort_custom
        SortMode.ALPHABETICAL -> R.string.sort_alphabetical
        SortMode.MOST_USED -> R.string.sort_most_used
        SortMode.RECENTLY_USED -> R.string.sort_recent
    },
)
