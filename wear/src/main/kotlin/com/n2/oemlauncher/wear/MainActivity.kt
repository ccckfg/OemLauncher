package com.n2.oemlauncher.wear

import android.os.Bundle
import android.content.Intent
import android.view.MotionEvent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.net.toUri
import com.n2.oemlauncher.wear.catalog.LauncherAppsObserver
import com.n2.oemlauncher.wear.catalog.AppListPermissionController
import com.n2.oemlauncher.wear.catalog.SAMSUNG_APP_LIST_PERMISSION
import com.n2.oemlauncher.wear.home.removeOtherLauncherTasks
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.ui.navigation.LauncherNavHost
import com.n2.oemlauncher.wear.ui.navigation.LauncherNavCallbacks
import com.n2.oemlauncher.wear.ui.theme.OemTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<LauncherViewModel>()
    private lateinit var appsObserver: LauncherAppsObserver
    private var hiddenByHome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        appsObserver.start()
        viewModel.onVisible()
        viewModel.refreshPlatformStatus(shouldShowPermissionRationale())
    }

    override fun onResume() {
        super.onResume()
        if (hiddenByHome && (application as App).container.foregroundState.snapshot().kind ==
            com.n2.oemlauncher.wear.home.ForegroundKind.WATCH_FACE
        ) {
            hiddenByHome = false
            (application as App).container.systemHomeResolver.resolveIntent()?.let { startActivity(it) }
            finishAndRemoveTask()
            return
        }
        hiddenByHome = false
        (application as App).container.foregroundState.setLauncherResumed(true)
        viewModel.refreshPlatformStatus(shouldShowPermissionRationale())
    }

    override fun onPause() {
        hiddenByHome = true
        super.onPause()
        if (!isChangingConfigurations) {
            removeOtherLauncherTasks(taskId)
            finish()
            finishAndRemoveTask()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        finishAndRemoveTask()
    }

    override fun onStop() {
        (application as App).container.foregroundState.setLauncherResumed(false)
        viewModel.onHidden()
        appsObserver.stop()
        // The crown button opens the system's recents over the launcher; coming back must not
        // land in a settings page the user already walked away from.
        viewModel.goHome()
        // The launcher is a transient application-list layer. Removing its task prevents Back on
        // the stock watch face from revealing a stale honeycomb underneath the system home task.
        finish()
        finishAndRemoveTask()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        hiddenByHome = false
        viewModel.goHome()
        viewModel.centerHoneycomb()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AppListPermissionController.REQUEST_CODE) {
            viewModel.refreshPlatformStatus(shouldShowPermissionRationale())
            viewModel.refresh()
        }
    }

    private fun shouldShowPermissionRationale(): Boolean =
        shouldShowRequestPermissionRationale(SAMSUNG_APP_LIST_PERMISSION)

    private fun requestAppListPermission() {
        viewModel.markAppListPermissionRequested(shouldShowPermissionRationale())
        (application as App).container.appListPermission.request(this)
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
