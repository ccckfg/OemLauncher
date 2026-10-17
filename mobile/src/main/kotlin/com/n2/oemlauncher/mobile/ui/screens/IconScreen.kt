package com.n2.oemlauncher.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.AppIcon
import com.n2.oemlauncher.mobile.ui.components.ChampagneButton
import com.n2.oemlauncher.mobile.ui.components.EmptyState
import com.n2.oemlauncher.mobile.ui.components.GlassButton
import com.n2.oemlauncher.mobile.ui.components.GlassRow
import com.n2.oemlauncher.mobile.ui.components.SearchPill

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
    val search by vm.searchIndex.collectAsStateWithLifecycle()
    val apps = remember(state.catalog, query, initialKey, search) {
        state.catalog?.entries.orEmpty().filter {
            search.matches(it, query) && (initialKey == null || it.key == initialKey)
        }
    }
    LazyColumn(
        Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
    ) {
        item {
            Text(
                stringResource(R.string.icon_tip),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            ChampagneButton(
                stringResource(R.string.import_zip),
                { zip.launch(ImportLimits.ZIP_MIME_TYPES) },
                Modifier.fillMaxWidth(),
            )
        }
        item {
            GlassButton(
                stringResource(R.string.restore_all),
                { vm.resetIcons() },
                Modifier.fillMaxWidth(),
                enabled = state.config.iconOverrides.isNotEmpty(),
            )
        }
        item { SearchPill(query) { query = it } }
        items(apps, key = { it.key }) { app ->
            GlassRow {
                AppIcon(app, state.config, vm)
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label, style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(if (app.key in state.config.iconOverrides) R.string.custom_icon else R.string.default_icon),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton({
                        target = app.key
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(
                            Icons.Outlined.PhotoLibrary, stringResource(R.string.choose_photo),
                            modifier = Modifier.size(ICON_DP.dp),
                        )
                    }
                    TextButton({ vm.resetIcon(app.key) }, enabled = app.key in state.config.iconOverrides) {
                        Icon(
                            Icons.Outlined.Restore, stringResource(R.string.restore),
                            modifier = Modifier.size(ICON_DP.dp),
                        )
                    }
                }
            }
        }
        if (apps.isEmpty()) item {
            EmptyState(
                message = stringResource(R.string.no_results),
                action = stringResource(R.string.clear_search),
                onAction = { query = "" },
            )
        }
        item { Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
    }
}

private const val ICON_DP = 22
