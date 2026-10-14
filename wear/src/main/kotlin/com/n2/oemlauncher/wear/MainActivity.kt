package com.n2.oemlauncher.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.n2.oemlauncher.wear.ui.LauncherScreen
import com.n2.oemlauncher.wear.ui.LauncherViewModel
import com.n2.oemlauncher.wear.catalog.LauncherAppsObserver

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<LauncherViewModel>()
    private lateinit var appsObserver: LauncherAppsObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appsObserver = LauncherAppsObserver(this) { viewModel.refresh() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.state.value.showSettings || viewModel.state.value.menuKey != null) {
                    viewModel.openSettings(false)
                    viewModel.showMenu(null)
                } else {
                    viewModel.centerHoneycomb()
                }
            }
        })
        setContent { LauncherScreen(viewModel) }
    }

    override fun onStart() {
        super.onStart()
        appsObserver.start()
    }

    override fun onStop() {
        appsObserver.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        viewModel.openSettings(false)
        viewModel.centerHoneycomb()
    }

}
