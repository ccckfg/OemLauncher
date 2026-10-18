package com.n2.oemlauncher.wear.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.AppGlyph
import com.n2.oemlauncher.wear.ui.LauncherUiState
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.ui.RotaryTicksScroll
import com.n2.oemlauncher.wear.ui.theme.oemBackdrop
import com.n2.oemlauncher.wear.ui.theme.OemColors

/** Long-press actions for one app as a scrolling page; swipe right to close it. */
@Composable
fun AppMenuScreen(key: String, state: LauncherUiState, viewModel: LauncherViewModel, onDone: () -> Unit) {
    val entry = state.entries.firstOrNull { it.key == key }
    if (entry == null) {
        LaunchedEffect(key) { onDone() }
        return
    }
    val favorite = key in state.config.favorites
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    RotaryTicksScroll(listState, state.rotaryTicks, state.config.layout.hapticsEnabled)
    ScreenScaffold(scrollState = listState, modifier = Modifier.oemBackdrop(state.config.layout.visualTier)) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                Column(
                    Modifier.fillMaxWidth().transformedHeight(this, spec).padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppGlyph(state.icons[key], Modifier.size(WearUiTokens.MENU_ICON_DP.dp))
                    Text(
                        entry.label,
                        Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                MenuButton(
                    icon = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    label = stringResource(if (favorite) R.string.favorite_remove else R.string.favorite_add),
                    spec = spec,
                ) {
                    viewModel.toggleFavorite(key)
                    onDone()
                }
            }
            item {
                MenuButton(Icons.Rounded.VisibilityOff, stringResource(R.string.hide_app), spec, iconTint = OemColors.Negative) {
                    viewModel.toggleHidden(key)
                    onDone()
                }
            }
            item {
                MenuButton(Icons.Rounded.Info, stringResource(R.string.app_info), spec) {
                    viewModel.openAppInfo(key)
                    onDone()
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.MenuButton(
    icon: ImageVector,
    label: String,
    spec: TransformationSpec,
    iconTint: androidx.compose.ui.graphics.Color = OemColors.TextPrimary,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        icon = { Icon(icon, contentDescription = null, tint = iconTint) },
        colors = androidx.wear.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = OemColors.Glass,
            contentColor = OemColors.TextPrimary,
            iconColor = iconTint,
        ),
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}
