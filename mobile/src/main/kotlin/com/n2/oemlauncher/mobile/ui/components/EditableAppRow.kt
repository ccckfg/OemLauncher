package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig

/**
 * One row of the editor: a glass capsule that lifts while it is dragged, a champagne star for
 * favorites, and the same reordering actions the sheet offers, for TalkBack.
 */
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
    val palette = GlassTheme.palette
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
    var dragging by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(
        targetValue = if (dragging) GlassTokens.DRAG_SCALE else 1f,
        animationSpec = spring(dampingRatio = GlassTokens.SPRING_DAMPING, stiffness = GlassTokens.SPRING_STIFFNESS),
        label = "lift",
    )
    val drag = Modifier.pointerInput(app.key, hidden) {
        if (!hidden) {
            var distance = 0f
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    distance = 0f
                    dragging = true
                    if (config.layout.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragEnd = { dragging = false },
                onDragCancel = { dragging = false },
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
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(GlassTokens.ROW_HEIGHT.dp)
                .graphicsLayer { scaleX = lift; scaleY = lift }
                .glassSurface(GlassTokens.ROW_CORNER.dp, elevated = dragging, refract = false)
                .then(drag).then(actions)
                .padding(horizontal = UiDimensions.SMALL_GAP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
        ) {
            if (!hidden) Icon(
                Icons.Outlined.DragHandle, dragLabel,
                tint = palette.textTertiary,
                modifier = Modifier.size(DRAG_ICON_DP.dp),
            )
            AppIcon(app, config, vm)
            Text(
                app.label, Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (!hidden) IconButton(favorite) {
                Icon(
                    if (pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    stringResource(if (pinned) R.string.remove_favorite else R.string.add_favorite, app.label),
                    tint = if (pinned) palette.accentText else palette.textTertiary,
                )
            }
            IconButton({ menu = true }) {
                Icon(
                    Icons.Outlined.MoreVert,
                    stringResource(R.string.more_actions, app.label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (menu) GlassBottomSheet({ menu = false }) {
            SheetTitle(app.label)
            if (!hidden) {
                SheetAction(
                    stringResource(R.string.action_move_up), Icons.Outlined.ArrowUpward,
                    enabled = canUp, onClick = { menu = false; move(-1) },
                )
                SheetAction(
                    stringResource(R.string.action_move_down), Icons.Outlined.ArrowDownward,
                    enabled = canDown, onClick = { menu = false; move(1) },
                )
            }
            SheetAction(stringResource(R.string.action_change_icon), Icons.Outlined.Image) {
                menu = false
                icon()
            }
            SheetAction(
                stringResource(if (hidden) R.string.action_show else R.string.action_hide),
                if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            ) { menu = false; hide() }
        }
    }
}

private const val DRAG_ICON_DP = 20
