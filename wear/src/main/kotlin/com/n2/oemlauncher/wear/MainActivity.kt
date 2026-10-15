package com.n2.oemlauncher.wear

import android.os.Bundle
import android.view.MotionEvent
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
        viewModel.onVisible()
    }

    override fun onStop() {
        viewModel.onHidden()
        appsObserver.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        viewModel.openSettings(false)
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
