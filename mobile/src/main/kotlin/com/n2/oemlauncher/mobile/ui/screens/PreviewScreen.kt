package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.GlassSegmented
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.shared.model.LayoutMode

@Composable
fun PreviewScreen(state: CompanionState, vm: CompanionViewModel) {
    Column(
        Modifier.padding(UiDimensions.PAGE_PADDING.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        Text(
            stringResource(R.string.preview_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WatchPreview(state, vm)
        GlassSegmented(
            options = LayoutMode.entries,
            selected = state.config.layout.mode,
            label = { stringResource(it.label()) },
            onSelect = { mode -> vm.layout { it.copy(mode = mode) } },
            description = stringResource(R.string.layout_mode),
        )
    }
}
