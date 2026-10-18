package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.catalog.EmptyCatalogState
import com.n2.oemlauncher.wear.catalog.emptyCatalogState
import com.n2.oemlauncher.wear.ui.theme.oemBackdrop
import com.n2.oemlauncher.wear.ui.theme.OemColors

/** Navigation out of the launcher home, provided by the navigation host. */
class HomeActions(
    val openSettings: () -> Unit,
    val openMenu: (String) -> Unit,
    val requestAppListPermission: () -> Unit,
    val openAppListPermissionSettings: () -> Unit,
)

/** Callbacks every home layout needs: launching is on the view model, the rest is navigation. */
class HomeCallbacks(
    val openSettings: () -> Unit,
    val openMenu: (String) -> Unit,
    val openSwitcher: () -> Unit,
)

@Composable
fun LauncherHome(state: LauncherUiState, viewModel: LauncherViewModel, actions: HomeActions) {
    var switcherOpen by rememberSaveable { mutableStateOf(false) }
    val callbacks = HomeCallbacks(actions.openSettings, actions.openMenu) { switcherOpen = true }
    val tier = state.config.layout.visualTier
    val blur = if (switcherOpen && tier == com.n2.oemlauncher.shared.model.VisualTier.REFINED) {
        com.n2.oemlauncher.shared.design.GlassSpecs.watch(tier).overlayBlurDp.dp
    } else 0.dp
    Box(Modifier.fillMaxSize().oemBackdrop(tier)) {
        Box(Modifier.fillMaxSize().blur(blur)) {
            when (state.config.layout.mode) {
                LayoutMode.HONEYCOMB -> HoneycombView(state, viewModel, callbacks)
                LayoutMode.GRID -> GridView(state, viewModel, callbacks)
                LayoutMode.LIST -> ListView(state, viewModel, callbacks)
            }
        }
        if (state.loading && state.entries.isEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else if (!state.loading && state.entries.isEmpty()) {
            val emptyState = emptyCatalogState(state.appListPermission)
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (emptyState == EmptyCatalogState.NEEDS_PERMISSION) {
                        stringResource(R.string.app_list_permission_message)
                    } else if (emptyState == EmptyCatalogState.PERMISSION_BLOCKED) {
                        stringResource(R.string.app_list_permission_blocked)
                    } else {
                        stringResource(R.string.empty_state)
                    },
                    color = OemColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                if (emptyState != EmptyCatalogState.GENERIC) {
                    FilledTonalButton(
                        onClick = if (emptyState == EmptyCatalogState.NEEDS_PERMISSION) {
                            actions.requestAppListPermission
                        } else {
                            actions.openAppListPermissionSettings
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            stringResource(
                                if (emptyState == EmptyCatalogState.NEEDS_PERMISSION) {
                                    R.string.app_list_permission_grant
                                } else {
                                    R.string.app_list_permission_settings
                                },
                            ),
                        )
                    }
                }
            }
        }
        state.message?.let {
            Text(
                it,
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = WearUiTokens.MESSAGE_BOTTOM_PADDING_DP.dp, start = 36.dp, end = 36.dp)
                    .background(OemColors.GlassStrong, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = OemColors.Negative,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
        if (switcherOpen) {
            LayoutSwitcher(
                current = state.config.layout.mode,
                tier = tier,
                onSelect = { mode ->
                    viewModel.setLayout(mode)
                    switcherOpen = false
                },
                onDismiss = { switcherOpen = false },
            )
        }
    }
}

/**
 * Settings entry at the start of the grid and the list, matching the gear that sits in the
 * honeycomb center; a long press opens the layout switcher there as well.
 */
@Composable
fun HomeSettingsButton(
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
    tier: VisualTier = VisualTier.REFINED,
) {
    val label = stringResource(R.string.settings)
    Box(modifier, contentAlignment = Alignment.Center) {
        CookieGearButton(
            label = label,
            tier = tier,
            onClick = callbacks.openSettings,
            onLongClick = callbacks.openSwitcher,
            longClickLabel = stringResource(R.string.switch_layout),
        )
    }
}
