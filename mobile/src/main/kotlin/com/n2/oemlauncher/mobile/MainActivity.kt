package com.n2.oemlauncher.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.navigation.CompanionApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CompanionApplication).container
        splash.setKeepOnScreenCondition { container.repository.state.value.loading }
        setContent {
            val vm: CompanionViewModel = viewModel(factory = CompanionViewModel.Factory(container))
            CompanionApp(vm)
        }
    }
}
