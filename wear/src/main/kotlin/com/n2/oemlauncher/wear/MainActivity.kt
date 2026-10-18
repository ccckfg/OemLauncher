package com.n2.oemlauncher.wear

import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.view.MotionEvent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.net.toUri
import com.n2.oemlauncher.wear.catalog.LauncherAppsObserver
import com.n2.oemlauncher.wear.catalog.SAMSUNG_APP_LIST_PERMISSION
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.ui.navigation.LauncherNavHost
import com.n2.oemlauncher.wear.ui.navigation.LauncherNavCallbacks
import com.n2.oemlauncher.wear.ui.theme.OemTheme
import com.n2.oemlauncher.wear.config.WearDebugLog
import com.n2.oemlauncher.wear.home.isCoveredInOwnTask

@SuppressLint("InvalidFragmentVersionForActivityResult")
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<LauncherViewModel>()
    private lateinit var appsObserver: LauncherAppsObserver
    private val appListPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshPlatformStatus(shouldShowPermissionRationale())
        viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearDebugLog.d { "activity onCreate saved=${savedInstanceState != null}" }
        appsObserver = LauncherAppsObserver(this) { viewModel.refresh() }
        // Registered before the navigation host, so it only runs on the home screen, where
        // the navigation host has nothing left to pop.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = viewModel.centerHoneycomb()
        })
        setContent {
            OemTheme {
                LauncherNavHost(
                    viewModel,
                    LauncherNavCallbacks(
                        openDefaultHomeSettings = ::openDefaultHomeSettings,
                        openAccessibilitySettings = ::openAccessibilitySettings,
                        requestAppListPermission = ::requestAppListPermission,
                        openAppListPermissionSettings = ::openAppListPermissionSettings,
                    ),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        WearDebugLog.d { "activity onStart" }
        appsObserver.start()
        viewModel.onVisible()
        viewModel.refreshPlatformStatus(shouldShowPermissionRationale())
    }

    override fun onResume() {
        super.onResume()
        WearDebugLog.d { "activity onResume" }
        val appContainer = (application as App).container
        appContainer.foregroundState.setLauncherResumed(true)
        appContainer.refreshSystemHomePackage()
        viewModel.refreshPlatformStatus(shouldShowPermissionRationale())
    }

    override fun onPause() {
        WearDebugLog.d { "activity onPause" }
        super.onPause()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        WearDebugLog.d { "activity onUserLeaveHint finish=home" }
        finishAndRemoveTask()
    }

    override fun onStop() {
        val covered = isCoveredInOwnTask()
        WearDebugLog.d { "activity onStop finish=${if (covered) "skip-covered" else "transient-launcher"}" }
        (application as App).container.foregroundState.setLauncherResumed(false)
        viewModel.onHidden()
        appsObserver.stop()
        // The crown button opens the system's recents over the launcher; coming back must not
        // land in a settings page the user already walked away from.
        viewModel.goHome()
        // The launcher is a transient application-list layer. Removing its task prevents Back on
        // the stock watch face from revealing a stale honeycomb underneath the system home task.
        if (!covered) {
            finish()
            finishAndRemoveTask()
        }
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        viewModel.goHome()
        viewModel.centerHoneycomb()
    }

    override fun onDestroy() {
        WearDebugLog.d { "activity onDestroy finishing=$isFinishing" }
        super.onDestroy()
    }

    private fun shouldShowPermissionRationale(): Boolean =
        shouldShowRequestPermissionRationale(SAMSUNG_APP_LIST_PERMISSION)

    private fun requestAppListPermission() {
        viewModel.markAppListPermissionRequested(shouldShowPermissionRationale())
        val controller = (application as App).container.appListPermission
        if (controller.isAvailable()) appListPermissionLauncher.launch(SAMSUNG_APP_LIST_PERMISSION)
    }

    private fun openAppListPermissionSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
    }

    private fun openDefaultHomeSettings() {
        val pm = packageManager
        val opened = com.n2.oemlauncher.wear.home.HomeSettingsRepository.homeSettingsIntents().any { intent ->
            if (pm.resolveActivity(intent, 0) == null) false
            else runCatching { startActivity(intent); true }.getOrDefault(false)
        }
        if (opened) viewModel.setHomeSettingsMessage(null)
        else viewModel.setHomeSettingsMessage(getString(com.n2.oemlauncher.wear.R.string.home_settings_unavailable))
    }

    private fun openAccessibilitySettings() {
        val opened = runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); true }.getOrDefault(false)
        if (opened) viewModel.setHomeSettingsMessage(null)
        else viewModel.setHomeSettingsMessage(getString(com.n2.oemlauncher.wear.R.string.home_settings_unavailable))
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            viewModel.onRotary(event.getAxisValue(MotionEvent.AXIS_SCROLL))
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}
