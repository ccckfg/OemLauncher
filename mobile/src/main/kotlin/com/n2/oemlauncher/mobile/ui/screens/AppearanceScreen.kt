package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.LayoutMode

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
        // The layout mode changes the whole preview, so its switch sits with the preview.
        val layoutChoice: @Composable () -> Unit = {
            GlassSegmented(
                options = LayoutMode.entries, selected = settings.mode,
                description = stringResource(R.string.group_layout), strong = true,
                label = { stringResource(it.label()) },
            ) { value -> vm.layout { it.copy(mode = value) } }
        }
        if (wide) {
            Row {
                AppearanceControls(settings, strength, { strength = it }, vm, Modifier.weight(1f))
                Column(
                    Modifier.width(UiDimensions.PREVIEW_PANE.dp).padding(UiDimensions.GAP.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
                ) {
                    WatchPreview(previewState, vm, maxEdgeDp = previewEdge)
                    layoutChoice()
                }
            }
        } else {
            Column {
                GlassCard(
                    modifier = Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
                    elevated = true,
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(GlassTokens.CARD_PADDING.dp),
                    ) {
                        WatchPreview(previewState, vm, diameterDp = GlassTokens.SMALL_PREVIEW)
                        layoutChoice()
                    }
                }
                AppearanceControls(settings, strength, { strength = it }, vm, Modifier.weight(1f))
            }
        }
    }
}
