package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig

@Composable
fun EditableAppRow(
    app: CatalogEntry,
    config: LauncherConfig,
    vm: CompanionViewModel,
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
    val upLabel = stringResource(R.string.move_up, app.label)
    val downLabel = stringResource(R.string.move_down, app.label)
    val hideLabel = stringResource(if (hidden) R.string.show_app else R.string.hide_app, app.label)
    val iconLabel = stringResource(R.string.change_icon, app.label)
    var menu by remember { mutableStateOf(false) }
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
    // TalkBack cannot drag; the same reorder is offered as custom actions on the row.
    val actions = Modifier.semantics {
        customActions = buildList {
            if (!hidden && canUp) add(CustomAccessibilityAction(upLabel) { currentMove.value(-1); true })
            if (!hidden && canDown) add(CustomAccessibilityAction(downLabel) { currentMove.value(1); true })
            add(CustomAccessibilityAction(hideLabel) { hide(); true })
            add(CustomAccessibilityAction(iconLabel) { icon(); true })
        }
    }
    Surface(modifier.fillMaxWidth().then(drag).then(actions), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier.padding(horizontal = UiDimensions.SMALL_GAP.dp, vertical = UiDimensions.SMALL_GAP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app, config, vm)
            Text(
                app.label, Modifier.weight(1f).padding(horizontal = UiDimensions.GAP.dp),
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (!hidden) IconButton(favorite) {
                Icon(
                    if (pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    stringResource(if (pinned) R.string.remove_favorite else R.string.add_favorite, app.label),
                    tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton({ menu = true }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_actions, app.label))
                }
                DropdownMenu(menu, { menu = false }) {
                    if (!hidden) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_move_up)) },
                            enabled = canUp, onClick = { menu = false; move(-1) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_move_down)) },
                            enabled = canDown, onClick = { menu = false; move(1) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_change_icon)) },
                        onClick = { menu = false; icon() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (hidden) R.string.action_show else R.string.action_hide)) },
                        onClick = { menu = false; hide() },
                    )
                }
            }
            if (!hidden) Icon(
                Icons.Outlined.DragHandle, dragLabel,
                modifier = Modifier.size(UiDimensions.TOUCH_TARGET.dp).padding(UiDimensions.GAP.dp),
            )
        }
    }
}
