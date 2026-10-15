package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.input.HapticController
import kotlin.math.abs

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
                if (state.config.layout.mode != LayoutMode.HONEYCOMB) {
                    SettingsEntry(
                        onClick = { viewModel.openSettings(true) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                    )
                }
                if (state.loading && state.entries.isEmpty()) Text(stringResource(R.string.loading), Modifier.align(Alignment.Center))
                if (!state.loading && state.entries.isEmpty()) Text(stringResource(R.string.empty_state), Modifier.align(Alignment.Center), color = Color.LightGray)
                state.message?.let { Text(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 74.dp), color = Color(0xFFFF8A80), fontSize = 12.sp) }
                state.menuKey?.let { AppMenuDialog(state, viewModel, it) }
            }
        }
    }
}

@Composable
private fun GridView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val columns = state.config.layout.gridColumns.coerceAtLeast(1)
    val rows = state.entries.chunked(columns)
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptics = remember { HapticController(context) }
    var lastRotaryTicks by remember { mutableIntStateOf(state.rotaryTicks) }
    LaunchedEffect(state.rotaryTicks) {
        val delta = state.rotaryTicks - lastRotaryTicks
        if (delta != 0) {
            listState.scrollBy(delta * 64f)
            repeat(abs(delta)) { haptics.tick(state.config.layout.hapticsEnabled) }
        }
        lastRotaryTicks = state.rotaryTicks
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Icons must stay perfectly round: cap them to the cell width the column count allows.
        val cellWidth = (maxWidth - GRID_HORIZONTAL_PADDING * 2 - GRID_SPACING * (columns - 1)) / columns
        val iconSize = minOf(HoneycombDefaults.iconDiameterDp(state.config.layout.iconSize).dp, cellWidth - CHIP_PADDING * 2)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = GRID_HORIZONTAL_PADDING, end = GRID_HORIZONTAL_PADDING, top = 26.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
        ) {
            itemsIndexed(rows, key = { _, row -> row.firstOrNull()?.key ?: row.hashCode() }) { _, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GRID_SPACING)) {
                    row.forEach { entry ->
                        AppChip(entry, state, viewModel, state.config.layout.showLabels, iconSize, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppChip(
    entry: CatalogEntry,
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    showLabel: Boolean,
    iconSize: Dp,
    modifier: Modifier,
) {
    Column(
        modifier.semantics { contentDescription = entry.label }
            .combinedClickable(onClick = { viewModel.launch(entry) }, onLongClick = { viewModel.showMenu(entry.key) })
            .padding(CHIP_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppGlyph(entry, state, Modifier.size(iconSize), state.icons[entry.key])
        if (showLabel) {
            Text(
                entry.label,
                Modifier.fillMaxWidth().padding(top = 2.dp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val GRID_HORIZONTAL_PADDING = 20.dp
private val GRID_SPACING = 6.dp
private val CHIP_PADDING = 2.dp
