package com.n2.oemlauncher.wear.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.n2.oemlauncher.wear.ui.HomeActions
import com.n2.oemlauncher.wear.ui.LauncherHome
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.ui.menu.AppMenuScreen
import com.n2.oemlauncher.wear.ui.settings.ChoiceScreen
import com.n2.oemlauncher.wear.ui.settings.SettingChoice
import com.n2.oemlauncher.wear.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ARG = "arg"
    const val CHOICE = "choice/{$ARG}"
    const val MENU = "menu/{$ARG}"

    fun choice(kind: SettingChoice) = "choice/${kind.name}"

    // App keys contain '/', which a route segment cannot carry unencoded.
    fun menu(key: String) = "menu/${Uri.encode(key)}"
}

data class LauncherNavCallbacks(
    val openDefaultHomeSettings: () -> Unit,
    val openAccessibilitySettings: () -> Unit,
    val requestAppListPermission: () -> Unit,
    val openAppListPermissionSettings: () -> Unit,
)

/**
 * Every screen above the launcher home lives in a swipe-to-dismiss container: a swipe to the
 * right from anywhere goes back, which also works with a mouse on the emulator.
 */
@Composable
fun LauncherNavHost(viewModel: LauncherViewModel, callbacks: LauncherNavCallbacks) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberSwipeDismissableNavController()
    LaunchedEffect(state.homeRequest) { nav.popToHome() }
    val home = remember(nav) {
        HomeActions(
            openSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            openMenu = { key -> nav.navigate(Routes.menu(key)) { launchSingleTop = true } },
            requestAppListPermission = callbacks.requestAppListPermission,
            openAppListPermissionSettings = callbacks.openAppListPermissionSettings,
        )
    }
    // Screens draw their own backdrop; a scaffold fill underneath would only be overdrawn.
    AppScaffold(timeText = { TimeText() }, containerColor = Color.Transparent) {
        SwipeDismissableNavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) { LauncherHome(state, viewModel, home) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onChoice = { nav.navigate(Routes.choice(it)) { launchSingleTop = true } },
                    onDone = { nav.popBackStack() },
                    onDefaultHomeSettings = callbacks.openDefaultHomeSettings,
                    onAccessibilitySettings = callbacks.openAccessibilitySettings,
                )
            }
            composable(Routes.CHOICE) { entry ->
                val kind = entry.arguments?.getString(Routes.ARG)
                    ?.let { name -> SettingChoice.entries.firstOrNull { it.name == name } }
                if (kind == null) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    ChoiceScreen(kind, state, viewModel, onDone = { nav.popBackStack() })
                }
            }
            composable(Routes.MENU) { entry ->
                val key = entry.arguments?.getString(Routes.ARG)?.let(Uri::decode).orEmpty()
                AppMenuScreen(key, state, viewModel, onDone = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.popToHome() {
    if (currentDestination?.route != Routes.HOME) popBackStack(Routes.HOME, inclusive = false)
}
