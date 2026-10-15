package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.n2.oemlauncher.shared.protocol.SyncPhase

/** One icon/color language for every status surface (home card, sync diagnostics). */
enum class StatusTone { GOOD, WAITING, WARNING, ERROR }

fun StatusTone.icon(): ImageVector = when (this) {
    StatusTone.GOOD -> Icons.Outlined.CheckCircle
    StatusTone.WAITING -> Icons.Outlined.Schedule
    StatusTone.WARNING -> Icons.Outlined.WarningAmber
    StatusTone.ERROR -> Icons.Outlined.ErrorOutline
}

@Composable
fun StatusTone.tint(): Color = when (this) {
    StatusTone.GOOD -> MaterialTheme.colorScheme.primary
    StatusTone.WAITING -> MaterialTheme.colorScheme.onSurfaceVariant
    StatusTone.WARNING -> MaterialTheme.colorScheme.tertiary
    StatusTone.ERROR -> MaterialTheme.colorScheme.error
}

fun SyncPhase.tone(): StatusTone = when (this) {
    SyncPhase.APPLIED -> StatusTone.GOOD
    SyncPhase.APPLIED_MISSING_ICONS -> StatusTone.WARNING
    SyncPhase.WATCH_OUTDATED -> StatusTone.WARNING
    SyncPhase.REJECTED -> StatusTone.ERROR
    SyncPhase.CONFLICT -> StatusTone.ERROR
    SyncPhase.NOT_SENT -> StatusTone.WAITING
    SyncPhase.WAITING_FOR_WATCH -> StatusTone.WAITING
}
