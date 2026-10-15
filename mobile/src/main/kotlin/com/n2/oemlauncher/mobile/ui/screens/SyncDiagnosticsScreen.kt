package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.ApiError
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.data.NodeStatus
import com.n2.oemlauncher.mobile.data.SyncDiagnostics
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.StatusTone
import com.n2.oemlauncher.mobile.ui.components.icon
import com.n2.oemlauncher.mobile.ui.components.relativeTime
import com.n2.oemlauncher.mobile.ui.components.tint

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
        item { NodesCard(diag.nodes) }
        item { ReceivedCard(diag) }
        item { SentCard(diag) }
        item { MissingCard(missing) }
        item { ErrorCard(diag.lastError) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
                OutlinedButton({ vm.requestCatalog() }, Modifier.weight(1f), enabled = !state.busy) {
                    Text(
                        stringResource(R.string.request_catalog),
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    )
                }
                Button({ vm.sync() }, Modifier.weight(1f), enabled = state.canSync && !state.conflict) {
                    Text(
                        stringResource(R.string.resend),
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item { Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
    }
}

@Composable
private fun DiagCard(title: Int, tone: StatusTone, content: @Composable ColumnScope.() -> Unit) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(UiDimensions.GAP.dp),
            verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(tone.icon(), null, tint = tone.tint())
                Text(
                    stringResource(title),
                    Modifier.padding(horizontal = UiDimensions.SMALL_GAP.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun KeyValueLine(label: Int, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun NodesCard(nodes: List<NodeStatus>) {
    val tone = when {
        nodes.any { it.hasWatchApp } -> StatusTone.GOOD
        nodes.isEmpty() -> StatusTone.WAITING
        else -> StatusTone.WARNING
    }
    DiagCard(R.string.diag_nodes, tone) {
        if (nodes.isEmpty()) InfoLine(stringResource(R.string.diag_no_nodes))
        nodes.forEach { node ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Watch, null,
                    tint = if (node.hasWatchApp) tone.tint() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.padding(horizontal = UiDimensions.SMALL_GAP.dp)) {
                    Text(node.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(
                            if (node.hasWatchApp) R.string.diag_node_ready else R.string.diag_node_missing,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceivedCard(diag: SyncDiagnostics) {
    val tone = if (diag.lastCatalogMillis > 0L && diag.lastStateMillis > 0L) {
        StatusTone.GOOD
    } else {
        StatusTone.WAITING
    }
    DiagCard(R.string.diag_received, tone) {
        KeyValueLine(R.string.diag_catalog_label, relativeTime(diag.lastCatalogMillis))
        KeyValueLine(R.string.diag_state_label, relativeTime(diag.lastStateMillis))
    }
}

@Composable
private fun SentCard(diag: SyncDiagnostics) {
    val tone = when (diag.lastSendSucceeded) {
        true -> StatusTone.GOOD
        false -> StatusTone.ERROR
        null -> StatusTone.WAITING
    }
    DiagCard(R.string.diag_sent, tone) {
        val revision = diag.lastSentRevision
        if (revision == null) {
            InfoLine(stringResource(R.string.diag_not_sent))
        } else {
            InfoLine(stringResource(R.string.diag_revision, revision))
            InfoLine(
                stringResource(
                    if (diag.lastSendSucceeded == true) R.string.diag_send_ok else R.string.diag_send_failed,
                ),
            )
        }
    }
}

@Composable
private fun MissingCard(missing: List<String>) {
    DiagCard(R.string.diag_missing, if (missing.isEmpty()) StatusTone.GOOD else StatusTone.WARNING) {
        if (missing.isEmpty()) {
            InfoLine(stringResource(R.string.diag_missing_none))
        } else {
            missing.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ErrorCard(error: ApiError?) {
    DiagCard(R.string.diag_error, if (error == null) StatusTone.GOOD else StatusTone.ERROR) {
        if (error == null) {
            InfoLine(stringResource(R.string.diag_error_none))
        } else {
            InfoLine(stringResource(R.string.diag_error_at, error.type, relativeTime(error.atMillis)))
        }
    }
}
