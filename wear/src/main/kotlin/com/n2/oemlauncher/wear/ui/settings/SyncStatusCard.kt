package com.n2.oemlauncher.wear.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.LauncherUiState
import com.n2.oemlauncher.wear.ui.theme.OemColors

/** Connection, last sync time and last applied result, tinted by connection state. */
@Composable
fun TransformingLazyColumnItemScope.SyncStatusCard(state: LauncherUiState, spec: TransformationSpec) {
    val colors = MaterialTheme.colorScheme
    val tint = when (state.syncConnected) {
        null -> colors.onSurfaceVariant
        true -> colors.primary
        false -> colors.error
    }
    val status = stringResource(
        when (state.syncConnected) {
            null -> R.string.sync_status_never
            true -> R.string.sync_status_connected
            false -> R.string.sync_status_disconnected
        },
    )
    val lastSync = state.syncLastText
    Column(
        Modifier.fillMaxWidth().transformedHeight(this, spec)
            .background(OemColors.Glass, MaterialTheme.shapes.large)
            .padding(WearUiTokens.CARD_PADDING_DP.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Sync, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(status, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
        }
        Text(
            if (lastSync == null) stringResource(R.string.sync_time_never) else stringResource(R.string.sync_time, lastSync),
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.sync_result, syncResultLabel(state)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun syncResultLabel(state: LauncherUiState): String = when (state.syncResult) {
    ApplyResult.NONE -> stringResource(R.string.sync_result_none)
    ApplyResult.APPLIED -> stringResource(R.string.sync_result_applied)
    ApplyResult.APPLIED_MISSING_ICONS -> stringResource(R.string.sync_result_missing_icons, state.syncMissingIcons)
    ApplyResult.REJECTED_STALE -> stringResource(R.string.sync_result_rejected_stale)
    ApplyResult.REJECTED_INVALID -> stringResource(R.string.sync_result_rejected_invalid)
    ApplyResult.REJECTED_SCHEMA -> stringResource(R.string.sync_result_rejected_schema)
}
