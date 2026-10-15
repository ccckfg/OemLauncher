package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.focusable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.wear.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.n2.oemlauncher.wear.input.HapticController
import androidx.compose.runtime.withFrameNanos

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val state by viewModel.state.collectAsState()
    OemTheme {
        if (state.showSettings) {
            SettingsScreen(state, viewModel)
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                when (state.config.layout.mode) {
                    LayoutMode.HONEYCOMB -> HoneycombView(state, viewModel)
                    LayoutMode.GRID -> GridView(state, viewModel)
                    LayoutMode.LIST -> ListView(state, viewModel)
                }
                val settingsAlignment = if (state.config.layout.mode == LayoutMode.HONEYCOMB) Alignment.BottomEnd else Alignment.TopCenter
                val settingsPadding = if (state.config.layout.mode == LayoutMode.HONEYCOMB) Modifier.padding(end = 16.dp, bottom = 44.dp) else Modifier.padding(top = 4.dp)
                val settingsSize = if (state.config.layout.mode == LayoutMode.HONEYCOMB) 48.dp else 40.dp
                Box(Modifier.align(settingsAlignment).then(settingsPadding).size(settingsSize).background(Color(0xFF10243A), androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).clickable { viewModel.openSettings(true) }) {
                    Text("⚙", Modifier.fillMaxSize().padding(8.dp), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, fontSize = 20.sp)
                }
                if (state.loading && state.entries.isEmpty()) Text(stringResource(R.string.loading), Modifier.align(Alignment.Center))
                if (!state.loading && state.entries.isEmpty()) Text(stringResource(R.string.empty_state), Modifier.align(Alignment.Center), color = Color.LightGray)
                state.message?.let { Text(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp), color = Color(0xFFFF8A80), fontSize = 12.sp) }
                state.menuKey?.let { AppMenuDialog(state, viewModel, it) }
            }
        }
    }
}

@Composable
private fun GridView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val rows = state.entries.chunked(state.config.layout.gridColumns)
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = remember { HapticController(context) }
    var lastRotaryTicks by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(state.rotaryTicks) }
    LaunchedEffect(state.rotaryTicks) {
        val delta = state.rotaryTicks - lastRotaryTicks
        if (delta != 0) {
            listState.scrollBy(delta * 64f)
            repeat(kotlin.math.abs(delta)) { haptics.tick(state.config.layout.hapticsEnabled) }
        }
        lastRotaryTicks = state.rotaryTicks
    }
    val rotaryModifier = launcherRotaryModifier(focusRequester, haptics, state.config.layout.hapticsEnabled) { delta -> scope.launch { listState.scrollBy(delta * 2f) } }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(top = 28.dp).then(rotaryModifier),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 44.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        itemsIndexed(rows, key = { _, row -> row.firstOrNull()?.key ?: row.hashCode() }) { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth().graphicsLayer {
                    if (rowIndex == 0 || rowIndex == rows.lastIndex) {
                        alpha = 0.72f
                        scaleX = 0.88f
                        scaleY = 0.88f
                    }
                },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { entry -> AppChip(entry, state, viewModel, state.config.layout.showLabels, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ListView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = remember { HapticController(context) }
    var lastRotaryTicks by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(state.rotaryTicks) }
    LaunchedEffect(state.rotaryTicks) {
        val delta = state.rotaryTicks - lastRotaryTicks
        if (delta != 0) {
            listState.scrollBy(delta * 64f)
            repeat(kotlin.math.abs(delta)) { haptics.tick(state.config.layout.hapticsEnabled) }
        }
        lastRotaryTicks = state.rotaryTicks
    }
    val rotaryModifier = launcherRotaryModifier(focusRequester, haptics, state.config.layout.hapticsEnabled) { delta -> scope.launch { listState.scrollBy(delta * 2f) } }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(top = 28.dp, start = 24.dp, end = 24.dp).then(rotaryModifier),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp),
    ) {
        items(state.entries, key = { it.key }) { entry ->
            val iconDp = with(HoneycombDefaults) { iconDiameterDp(state.config.layout.iconSize).dp }
            Row(
                Modifier.fillMaxWidth().semantics { contentDescription = entry.label }.combinedClickable(
                    onClick = { viewModel.launch(entry) },
                    onLongClick = { viewModel.showMenu(entry.key) },
                ).padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppGlyph(entry, state, Modifier.size(iconDp), state.icons[entry.key])
                if (state.config.layout.showLabels) {
                    Text(entry.label, Modifier.padding(start = 10.dp), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AppChip(entry: CatalogEntry, state: LauncherUiState, viewModel: LauncherViewModel, showLabel: Boolean, modifier: Modifier) {
    val iconDp = HoneycombDefaults.iconDiameterDp(state.config.layout.iconSize).dp
    Column(
        modifier.semantics { contentDescription = entry.label }.combinedClickable(onClick = { viewModel.launch(entry) }, onLongClick = { viewModel.showMenu(entry.key) }).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppGlyph(entry, state, Modifier.size(iconDp), state.icons[entry.key])
        if (showLabel) Text(entry.label, Modifier.fillMaxWidth().padding(top = 3.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun launcherRotaryModifier(focusRequester: FocusRequester, haptics: HapticController, hapticsEnabled: Boolean, onDelta: (Float) -> Unit): Modifier {
    LaunchedEffect(focusRequester) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
    return Modifier.onRotaryScrollEvent { event ->
        onDelta(event.verticalScrollPixels)
        if (abs(event.verticalScrollPixels) > 0.1f) haptics.tick(hapticsEnabled)
        true
    }.focusRequester(focusRequester).focusable()
}
