package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
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
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.theme.oemBackdrop

/** Navigation out of the launcher home, provided by the navigation host. */
class HomeActions(val openSettings: () -> Unit, val openMenu: (String) -> Unit)

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
    val blur = if (switcherOpen) WearUiTokens.SWITCHER_BLUR_DP.dp else 0.dp
    Box(Modifier.fillMaxSize().oemBackdrop()) {
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
            Text(
                stringResource(R.string.empty_state),
                Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        state.message?.let {
            Text(
                it,
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = WearUiTokens.MESSAGE_BOTTOM_PADDING_DP.dp, start = 36.dp, end = 36.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
        if (switcherOpen) {
            LayoutSwitcher(
                current = state.config.layout.mode,
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
fun HomeSettingsButton(callbacks: HomeCallbacks, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.settings)
    Box(modifier, contentAlignment = Alignment.Center) {
        FilledTonalIconButton(
            onClick = callbacks.openSettings,
            onLongClick = callbacks.openSwitcher,
            onLongClickLabel = stringResource(R.string.switch_layout),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = label)
        }
    }
}
