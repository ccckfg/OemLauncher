package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.editor.ImportState
import com.n2.oemlauncher.mobile.ui.components.AppIcon
import com.n2.oemlauncher.mobile.ui.components.Processing

@Composable
fun ImportResultScreen(state: CompanionState, imports: ImportState, vm: CompanionViewModel, done: () -> Unit) {
    val preview = imports.preview
    LazyColumn(
        Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        item {
            if (imports.busy) Processing()
            else if (preview == null) Text(stringResource(R.string.nothing_imported))
            else Column {
                Text(stringResource(R.string.import_matched_count, preview.matches.size), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.import_unmatched_count, preview.unmatched.size), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.import_hint))
                Button(
                    { vm.applyImport(done) }, Modifier.fillMaxWidth(),
                    enabled = preview.matches.isNotEmpty(),
                ) { Text(stringResource(R.string.apply_matches)) }
            }
            TextButton({ vm.cancelImport(); done() }) { Text(stringResource(R.string.cancel)) }
        }
        if (preview != null) {
            items(preview.matches, key = { it.app.key }) { match ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(match.app, state.config, vm.container.icons, match.iconId)
                    Text(match.app.label, Modifier.padding(horizontal = UiDimensions.GAP.dp))
                }
            }
            if (preview.unmatched.isNotEmpty()) item {
                Text(stringResource(R.string.unmatched), style = MaterialTheme.typography.titleMedium)
            }
            items(preview.unmatched, key = { it }) { Text(it) }
        }
    }
}
