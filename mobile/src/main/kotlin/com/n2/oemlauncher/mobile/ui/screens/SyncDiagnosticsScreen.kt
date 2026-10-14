package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.Section
import java.text.DateFormat
import java.util.Date

@Composable
fun SyncDiagnosticsScreen(state: CompanionState, vm: CompanionViewModel) {
    val diag = state.diagnostics
    val missing = remember(diag.localMissingIcons, state.snapshot.watch) {
        (diag.localMissingIcons + state.snapshot.watch?.missingIconIds.orEmpty()).distinct()
    }
    LazyColumn(
        Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        item {
            Section(stringResource(R.string.diag_nodes)) {
                if (diag.nodes.isEmpty()) Text(stringResource(R.string.diag_no_nodes))
                diag.nodes.forEach { node ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Watch, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(horizontal = UiDimensions.GAP.dp)) {
                            Text(node.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(
                                    if (node.hasWatchApp) R.string.diag_node_ready else R.string.diag_node_missing,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        item {
            Section(stringResource(R.string.diag_received)) {
                Text(stringResource(R.string.diag_catalog_at, timeText(diag.lastCatalogMillis)))
                Text(stringResource(R.string.diag_state_at, timeText(diag.lastStateMillis)))
            }
        }
        item {
            Section(stringResource(R.string.diag_sent)) {
                val revision = diag.lastSentRevision
                if (revision == null) {
                    Text(stringResource(R.string.diag_not_sent))
                } else {
                    Text(stringResource(R.string.diag_revision, revision))
                    Text(
                        stringResource(
                            if (diag.lastSendSucceeded == true) R.string.diag_send_ok else R.string.diag_send_failed,
                        ),
                    )
                }
            }
        }
        item {
            Section(stringResource(R.string.diag_missing)) {
                if (missing.isEmpty()) Text(stringResource(R.string.diag_missing_none))
                missing.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            Section(stringResource(R.string.diag_error)) {
                val error = diag.lastError
                if (error == null) Text(stringResource(R.string.diag_error_none))
                else Text(stringResource(R.string.diag_error_at, error.type, timeText(error.atMillis)))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
                OutlinedButton({ vm.requestCatalog() }, Modifier.weight(1f), enabled = !state.busy) {
                    Text(stringResource(R.string.request_catalog))
                }
                Button({ vm.sync() }, Modifier.weight(1f), enabled = state.canSync && !state.conflict) {
                    Text(stringResource(R.string.resend))
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(UiDimensions.GAP.dp)) }
    }
}

@Composable
private fun timeText(millis: Long): String = if (millis == 0L) {
    stringResource(R.string.diag_never)
} else {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
}
