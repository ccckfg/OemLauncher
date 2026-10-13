package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.IconStore
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig

@Composable
fun EditableAppRow(
    app: CatalogEntry,
    config: LauncherConfig,
    store: IconStore,
    canUp: Boolean,
    canDown: Boolean,
    favorite: () -> Unit,
    hide: () -> Unit,
    icon: () -> Unit,
    move: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pinned = app.key in config.favorites
    val hidden = app.key in config.hidden
    val currentMove = rememberUpdatedState(move)
    val threshold = with(LocalDensity.current) { UiDimensions.DRAG_THRESHOLD.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val dragLabel = stringResource(R.string.drag_app, app.label)
    val drag = Modifier.pointerInput(app.key, hidden) {
        if (!hidden) {
            var distance = 0f
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    distance = 0f
                    if (config.layout.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, amount ->
                    change.consume()
                    distance += amount.y
                    if (distance > threshold) { currentMove.value(1); distance -= threshold }
                    if (distance < -threshold) { currentMove.value(-1); distance += threshold }
                },
            )
        }
    }
    Surface(modifier.fillMaxWidth().then(drag), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = UiDimensions.SMALL_GAP.dp, vertical = UiDimensions.SMALL_GAP.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, config, store)
                Text(
                    app.label, Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp),
                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (!hidden) Icon(
                    Icons.Outlined.DragHandle, dragLabel,
                    modifier = Modifier.size(UiDimensions.TOUCH_TARGET.dp).padding(UiDimensions.GAP.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(favorite) {
                    Icon(
                        if (pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        stringResource(if (pinned) R.string.remove_favorite else R.string.add_favorite, app.label),
                        tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(hide) {
                    Icon(
                        if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        stringResource(if (hidden) R.string.show_app else R.string.hide_app, app.label),
                    )
                }
                IconButton(icon) { Icon(Icons.Outlined.Image, stringResource(R.string.change_icon, app.label)) }
                IconButton({ move(-1) }, enabled = canUp && !hidden) {
                    Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.move_up, app.label))
                }
                IconButton({ move(1) }, enabled = canDown && !hidden) {
                    Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.move_down, app.label))
                }
            }
        }
    }
}
