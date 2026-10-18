package com.n2.oemlauncher.mobile.ui.components

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.ThemeTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme

/**
 * Glass dialog on a card corner. On Android 12+ with cross-window blur the window blurs what
 * is behind it; everywhere else the scrim carries the separation instead.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = GlassTheme.palette
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val blurred = overlayBlurBehind(ThemeTokens.OVERLAY_BLUR_DP)
        val scrim = Color.Black.copy(alpha = if (blurred) BLURRED_SCRIM_ALPHA else GlassTokens.SCRIM_ALPHA)
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier.fillMaxSize().background(scrim)
                .clickable(interactionSource = interaction, indication = null, onClick = onDismissRequest),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(UiDimensions.PAGE_PADDING.dp)
                    .glassSurface(GlassTokens.CARD_CORNER.dp, strong = true, elevated = true)
                    .clickable(interactionSource = interaction, indication = null, onClick = {})
                    .padding(UiDimensions.PAGE_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
                content = content,
            )
        }
    }
}

/** Glass bottom sheet; the same blur rule as the dialog, with a scrim as the fallback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = GlassTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = GlassTokens.SHEET_CORNER.dp, topEnd = GlassTokens.SHEET_CORNER.dp),
        containerColor = palette.strongOverBackground,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = GlassTokens.SCRIM_ALPHA),
        dragHandle = { SheetHandle() },
    ) {
        overlayBlurBehind(ThemeTokens.OVERLAY_BLUR_DP)
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

/** A row of a sheet: an action with an icon, as tall as a list row. */
@Composable
fun SheetAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().height(GlassTokens.ROW_HEIGHT.dp)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = GlassTokens.CARD_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        Icon(
            icon, null,
            tint = tint ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(SHEET_ICON_DP.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.fillMaxWidth().height(HANDLE_AREA_DP.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(HANDLE_WIDTH_DP.dp, HANDLE_HEIGHT_DP.dp)
                .background(GlassTheme.palette.glassStrong, RoundedCornerShape(50)),
        )
    }
}

/**
 * Applies [radiusDp] of background blur to the window this composable lives in, when the
 * platform supports cross-window blur. Returns whether the blur took effect.
 */
@Composable
private fun overlayBlurBehind(radiusDp: Float): Boolean {
    val view = LocalView.current
    val density = LocalDensity.current
    var applied by remember { mutableStateOf(false) }
    val radiusPx = with(density) { radiusDp.dp.roundToPx() }
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    SideEffect {
        applied = supported && blurWindow(view, radiusPx)
    }
    return applied
}

@RequiresApi(Build.VERSION_CODES.S)
private fun blurWindow(view: View, radiusPx: Int): Boolean {
    val window = (view.parent as? DialogWindowProvider)?.window ?: return false
    val manager = view.context.getSystemService(WindowManager::class.java) ?: return false
    if (!manager.isCrossWindowBlurEnabled) return false
    window.setBackgroundBlurRadius(radiusPx)
    return true
}

private const val BLURRED_SCRIM_ALPHA = 0.35f
private const val HANDLE_AREA_DP = 28
private const val HANDLE_WIDTH_DP = 32
private const val HANDLE_HEIGHT_DP = 4
private const val SHEET_ICON_DP = 22
