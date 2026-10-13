package com.n2.oemlauncher.mobile.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.UserNotice
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.mobile.ui.screens.AppearanceScreen
import com.n2.oemlauncher.mobile.ui.screens.CropScreen
import com.n2.oemlauncher.mobile.ui.screens.EditorScreen
import com.n2.oemlauncher.mobile.ui.screens.HelpScreen
import com.n2.oemlauncher.mobile.ui.screens.HomeDestination
import com.n2.oemlauncher.mobile.ui.screens.HomeScreen
import com.n2.oemlauncher.mobile.ui.screens.IconScreen
import com.n2.oemlauncher.mobile.ui.screens.ImportResultScreen
import com.n2.oemlauncher.mobile.ui.screens.PreviewScreen
import com.n2.oemlauncher.mobile.ui.theme.CompanionTheme
import com.n2.oemlauncher.mobile.ui.theme.MatchSystemBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionApp(vm: CompanionViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val imports by vm.importState.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val home = destination == null || destination.hasRoute<Home>()
    var dark by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val systemDark = isSystemInDarkTheme()
    val editing = destination?.let {
        it.hasRoute<Editor>() || it.hasRoute<Appearance>() || it.hasRoute<IconsPage>() || it.hasRoute<Preview>()
    } ?: false
    val importing = destination?.let { it.hasRoute<Crop>() || it.hasRoute<ImportResult>() } ?: false
    val back: () -> Unit = {
        if (importing) vm.cancelImport()
        nav.popBackStack()
    }
    BackHandler(enabled = importing, onBack = back)
    val title = when {
        home -> R.string.app_name
        destination.hasRoute<Editor>() -> R.string.layout_editor
        destination.hasRoute<Appearance>() -> R.string.appearance
        destination.hasRoute<IconsPage>() -> R.string.icons
        destination.hasRoute<Preview>() -> R.string.preview
        destination.hasRoute<Crop>() -> R.string.crop
        destination.hasRoute<ImportResult>() -> R.string.import_result
        else -> R.string.help
    }
    LifecycleStartEffect(vm) {
        vm.start()
        onStopOrDispose { vm.stop() }
    }
    CompanionTheme(dark) {
        MatchSystemBars()
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (!home) IconButton(back) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (editing) {
                        IconButton({ vm.undo() }, enabled = state.draft?.undo?.isNotEmpty() == true) {
                            Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo))
                        }
                        IconButton({ vm.redo() }, enabled = state.draft?.redo?.isNotEmpty() == true) {
                            Icon(Icons.AutoMirrored.Outlined.Redo, stringResource(R.string.redo))
                        }
                    }
                    IconButton({ dark = !(dark ?: systemDark) }) {
                        Icon(Icons.Outlined.Contrast, stringResource(R.string.theme))
                    }
                },
            )
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(
                    navController = nav, startDestination = Home,
                    enterTransition = {
                        if (state.config.layout.reduceMotion) EnterTransition.None
                        else fadeIn(tween(UiDimensions.ANIMATION_MS))
                    },
                    exitTransition = {
                        if (state.config.layout.reduceMotion) ExitTransition.None
                        else fadeOut(tween(UiDimensions.ANIMATION_MS))
                    },
                ) {
                    composable<Home> {
                        HomeScreen(state, vm) {
                            when (it) {
                                HomeDestination.EDITOR -> nav.navigate(Editor)
                                HomeDestination.APPEARANCE -> nav.navigate(Appearance)
                                HomeDestination.ICONS -> nav.navigate(IconsPage())
                                HomeDestination.PREVIEW -> nav.navigate(Preview)
                                HomeDestination.HELP -> nav.navigate(Help)
                            }
                        }
                    }
                    composable<Editor> {
                        EditorScreen(state, vm, { nav.navigate(IconsPage(it)) }, { nav.navigate(Preview) })
                    }
                    composable<Appearance> { AppearanceScreen(state, vm) }
                    composable<IconsPage> {
                        IconScreen(state, vm, it.toRoute<IconsPage>().appKey, { key ->
                            nav.navigate(Crop(key))
                        }, { nav.navigate(ImportResult) })
                    }
                    composable<Preview> { PreviewScreen(state, vm) }
                    composable<Crop> { CropScreen(it.toRoute<Crop>().appKey, vm, imports) { nav.popBackStack() } }
                    composable<ImportResult> { ImportResultScreen(state, imports, vm) { nav.popBackStack() } }
                    composable<Help> { HelpScreen() }
                }
            }
        }
        if (state.notice != UserNotice.NONE) AlertDialog(
            onDismissRequest = vm::dismissNotice,
            text = { Text(stringResource(state.notice.label())) },
            confirmButton = { TextButton(vm::dismissNotice) { Text(stringResource(R.string.dismiss)) } },
        )
    }
}
