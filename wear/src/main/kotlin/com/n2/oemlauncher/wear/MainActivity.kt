package com.n2.oemlauncher.wear

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.n2.oemlauncher.wear.catalog.LauncherAppsObserver
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.ui.navigation.LauncherNavHost
import com.n2.oemlauncher.wear.ui.theme.OemTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<LauncherViewModel>()
    private lateinit var appsObserver: LauncherAppsObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appsObserver = LauncherAppsObserver(this) { viewModel.refresh() }
        // Registered before the navigation host, so it only runs on the home screen, where
        // the navigation host has nothing left to pop.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = viewModel.centerHoneycomb()
        })
        setContent { OemTheme { LauncherNavHost(viewModel) } }
    }

    override fun onStart() {
        super.onStart()
        appsObserver.start()
        viewModel.onVisible()
    }

    override fun onStop() {
        viewModel.onHidden()
        appsObserver.stop()
        // The crown button opens the system's recents over the launcher; coming back must not
        // land in a settings page the user already walked away from.
        viewModel.goHome()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        viewModel.goHome()
        viewModel.centerHoneycomb()
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            viewModel.onRotary(event.getAxisValue(MotionEvent.AXIS_SCROLL))
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}
