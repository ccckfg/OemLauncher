package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.data.ConnectionKind
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.icon
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.mobile.ui.components.relativeTime
import com.n2.oemlauncher.mobile.ui.components.tint
import com.n2.oemlauncher.mobile.ui.components.tone

@Composable
fun HomeScreen(state: CompanionState, vm: CompanionViewModel, navigate: (HomeDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.home), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val catalog = state.catalog
        if (catalog == null) item {
            Card {
                Column(
                    Modifier.padding(UiDimensions.PAGE_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
                ) {
                    Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.empty_detail), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { vm.demo() }) { Text(stringResource(R.string.try_demo)) }
                }
            }
        } else item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                WatchPreview(state, vm, thumbnail = true, onOpen = { navigate(HomeDestination.PREVIEW) })
                Text(
                    stringResource(R.string.preview_tap),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.demo) item { DemoBanner() }
        item { StatusCard(state, vm) { navigate(HomeDestination.SYNC) } }
        item {
            Column {
                NavigationEntry(R.string.layout_editor, R.string.layout_editor_detail, Icons.AutoMirrored.Outlined.Sort, catalog != null) {
                    navigate(HomeDestination.EDITOR)
                }
                NavigationEntry(R.string.appearance, R.string.appearance_detail, Icons.Outlined.Widgets, catalog != null) {
                    navigate(HomeDestination.APPEARANCE)
                }
                NavigationEntry(R.string.icons, R.string.icons_detail, Icons.Outlined.Palette, catalog != null) {
                    navigate(HomeDestination.ICONS)
                }
                NavigationEntry(R.string.help, R.string.help_detail, Icons.AutoMirrored.Outlined.HelpOutline, true) {
                    navigate(HomeDestination.HELP)
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
    }
}

enum class HomeDestination { EDITOR, APPEARANCE, ICONS, PREVIEW, SYNC, HELP }

@Composable
private fun DemoBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = UiDimensions.GAP.dp, vertical = UiDimensions.SMALL_GAP.dp),
        ) {
            Text(stringResource(R.string.demo), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.demo_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = UiDimensions.DETAIL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Connection and sync in one compact card; no disabled buttons, actions appear when useful. */
@Composable
private fun StatusCard(state: CompanionState, vm: CompanionViewModel, diagnostics: () -> Unit) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(UiDimensions.GAP.dp),
            verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
        ) {
            ConnectionRow(state, vm::refresh)
            SyncRow(state, vm)
            if (state.conflict) ConflictRow(state, vm)
            DiagnosticsRow(diagnostics)
        }
    }
}

@Composable
private fun ConnectionRow(state: CompanionState, refresh: () -> Unit) {
    val title = when (state.connection.kind) {
        ConnectionKind.UNDISCOVERED -> R.string.no_watch
        ConnectionKind.CONNECTED -> R.string.connected
        ConnectionKind.APP_MISSING -> R.string.app_missing
        ConnectionKind.PLAY_SERVICES_MISSING -> R.string.play_missing
    }
    val catalog = state.catalog
    val detail = when {
        state.connection.kind == ConnectionKind.CONNECTED && catalog != null -> stringResource(
            R.string.watch_versions, catalog.deviceName, catalog.sdkInt, catalog.appVersionName,
        )
        state.connection.kind == ConnectionKind.CONNECTED -> stringResource(R.string.connected_detail)
        state.connection.kind == ConnectionKind.APP_MISSING -> stringResource(R.string.app_missing_detail)
        state.connection.kind == ConnectionKind.PLAY_SERVICES_MISSING -> stringResource(R.string.play_missing_detail)
        else -> stringResource(R.string.no_watch_detail)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Watch, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = UiDimensions.DETAIL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(refresh) { Icon(Icons.Outlined.Refresh, stringResource(R.string.refresh)) }
    }
}

@Composable
private fun SyncRow(state: CompanionState, vm: CompanionViewModel) {
    val tone = state.phase.tone()
    val dirty = state.draft?.dirty == true
    val canSend = state.canSync && !state.conflict
    val subtitle = when {
        state.conflict -> null
        dirty -> stringResource(R.string.unsent_changes)
        state.canSync -> stringResource(R.string.up_to_date)
        state.snapshot.lastSyncMillis == 0L -> stringResource(R.string.never_synced)
        else -> stringResource(R.string.last_sync, relativeTime(state.snapshot.lastSyncMillis))
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(tone.icon(), null, tint = tone.tint())
        Column(Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp)) {
            Text(
                stringResource(if (state.conflict) R.string.sync_conflict else state.phase.label()),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = UiDimensions.DETAIL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canSend && dirty) TextButton({ vm.sync() }) {
            Text(stringResource(R.string.sync_changes), maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun ConflictRow(state: CompanionState, vm: CompanionViewModel) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
        Text(
            stringResource(R.string.conflict_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
            TextButton({ vm.loadWatch() }) { Text(stringResource(R.string.load_watch), maxLines = 1, softWrap = false) }
            OutlinedButton({ vm.sync(overwrite = true) }, enabled = state.canSync) {
                Text(stringResource(R.string.overwrite_watch), maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(open: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = open).padding(vertical = UiDimensions.GAP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Insights, null, tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.sync_diagnostics),
            Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NavigationEntry(title: Int, detail: Int, icon: ImageVector, enabled: Boolean, action: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = action)
            .padding(vertical = UiDimensions.SMALL_GAP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
