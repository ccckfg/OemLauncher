package com.n2.oemlauncher.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.AppIcon
import com.n2.oemlauncher.mobile.ui.components.SearchField

@Composable
fun IconScreen(
    state: CompanionState, vm: CompanionViewModel,
    initialKey: String?, crop: (String) -> Unit, importResult: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf(initialKey) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val key = target
        if (uri != null && key != null) { vm.photo(uri); crop(key) }
    }
    val zip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { vm.importZip(uri); importResult() }
    }
    val apps = remember(state.catalog, query, initialKey) {
        state.catalog?.entries.orEmpty().filter {
            it.label.contains(query, true) && (initialKey == null || it.key == initialKey)
        }
    }
    LazyColumn(
        Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        item {
            Text(stringResource(R.string.icon_tip), style = MaterialTheme.typography.bodyMedium)
            Button({ zip.launch(ImportLimits.ZIP_MIME_TYPES) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_zip))
            }
            OutlinedButton({ vm.resetIcons() }, Modifier.fillMaxWidth(), enabled = state.config.iconOverrides.isNotEmpty()) {
                Text(stringResource(R.string.restore_all))
            }
        }
        item { SearchField(query) { query = it } }
        items(apps, key = { it.key }) { app ->
            Card {
                Column(Modifier.padding(UiDimensions.GAP.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(app, state.config, vm.container.icons)
                        Column(Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp)) {
                            Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(if (app.key in state.config.iconOverrides) R.string.custom_icon else R.string.default_icon),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row {
                        TextButton({
                            target = app.key
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text(stringResource(R.string.choose_photo)) }
                        TextButton({ vm.resetIcon(app.key) }, enabled = app.key in state.config.iconOverrides) {
                            Text(stringResource(R.string.restore))
                        }
                    }
                }
            }
        }
        if (apps.isEmpty()) item { Text(stringResource(R.string.no_results)) }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(UiDimensions.GAP.dp)) }
    }
}
