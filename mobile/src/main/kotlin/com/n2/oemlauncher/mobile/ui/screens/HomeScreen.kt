package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.Sort
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.data.ConnectionKind
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(state: CompanionState, vm: CompanionViewModel, navigate: (HomeDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.home), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { ConnectionCard(state, vm::refresh) }
        item { SyncCard(state, vm) }
        if (state.demo) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(UiDimensions.GAP.dp)) {
                    Text(stringResource(R.string.demo), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.demo_detail), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.catalog == null) item {
            Card {
                Column(Modifier.padding(UiDimensions.PAGE_PADDING.dp), verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
                    Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.empty_detail))
                    Button(onClick = { vm.demo() }) { Text(stringResource(R.string.try_demo)) }
                }
            }
        } else item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                WatchPreview(state, vm.container.icons, thumbnail = true)
                TextButton({ navigate(HomeDestination.PREVIEW) }) { Text(stringResource(R.string.preview)) }
            }
        }
        item {
            NavigationEntry(R.string.layout_editor, R.string.layout_editor_detail, Icons.AutoMirrored.Outlined.Sort, state.catalog != null) {
                navigate(HomeDestination.EDITOR)
            }
            NavigationEntry(R.string.appearance, R.string.appearance_detail, Icons.Outlined.Widgets, state.catalog != null) {
                navigate(HomeDestination.APPEARANCE)
            }
            NavigationEntry(R.string.icons, R.string.icons_detail, Icons.Outlined.Palette, state.catalog != null) {
                navigate(HomeDestination.ICONS)
            }
            NavigationEntry(R.string.help, R.string.help_detail, Icons.AutoMirrored.Outlined.HelpOutline, true) {
                navigate(HomeDestination.HELP)
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
    }
}

enum class HomeDestination { EDITOR, APPEARANCE, ICONS, PREVIEW, HELP }

@Composable
private fun ConnectionCard(state: CompanionState, refresh: () -> Unit) {
    val (title, detail) = when (state.connection.kind) {
        ConnectionKind.UNDISCOVERED -> R.string.no_watch to R.string.no_watch_detail
        ConnectionKind.CONNECTED -> R.string.connected to R.string.connected
        ConnectionKind.APP_MISSING -> R.string.app_missing to R.string.app_missing_detail
        ConnectionKind.PLAY_SERVICES_MISSING -> R.string.play_missing to R.string.play_missing_detail
    }
    Card {
        Row(Modifier.fillMaxWidth().padding(UiDimensions.GAP.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Watch, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                if (state.connection.kind == ConnectionKind.CONNECTED && state.catalog != null) {
                    val catalog = requireNotNull(state.catalog)
                    Text(
                        stringResource(R.string.watch_versions, catalog.deviceName, catalog.sdkInt, catalog.appVersionName),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else Text(stringResource(detail), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(refresh) { Icon(Icons.Outlined.Refresh, stringResource(R.string.refresh)) }
        }
    }
}

@Composable
private fun SyncCard(state: CompanionState, vm: CompanionViewModel) {
    val lastSync = remember(state.snapshot.lastSyncMillis) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(state.snapshot.lastSyncMillis))
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (state.draft?.dirty == true) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(UiDimensions.GAP.dp),
            verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
        ) {
            Text(stringResource(if (state.conflict) R.string.sync_conflict else state.phase.label()), style = MaterialTheme.typography.titleMedium)
            Text(
                if (state.snapshot.lastSyncMillis == 0L) stringResource(R.string.never_synced)
                else stringResource(R.string.last_sync, lastSync),
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.draft?.dirty == true) Text(stringResource(R.string.unsent_changes), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { vm.sync() }, enabled = state.canSync && !state.conflict, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.draft?.dirty == true) R.string.sync_changes else R.string.sync))
            }
            AnimatedVisibility(state.conflict) {
                Column {
                    Text(stringResource(R.string.conflict_detail))
                    TextButton({ vm.loadWatch() }) { Text(stringResource(R.string.load_watch)) }
                    OutlinedButton({ vm.sync(overwrite = true) }, enabled = state.canSync) {
                        Text(stringResource(R.string.overwrite_watch))
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationEntry(title: Int, detail: Int, icon: ImageVector, enabled: Boolean, action: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = action).padding(vertical = UiDimensions.GAP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, null)
    }
}
