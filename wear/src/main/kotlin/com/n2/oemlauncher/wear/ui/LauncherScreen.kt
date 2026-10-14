package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.wear.R

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val state by viewModel.state.collectAsState()
    OemTheme {
        if (state.showSettings) SettingsScreen(state, viewModel) else Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (state.config.layout.mode) {
                LayoutMode.HONEYCOMB -> HoneycombView(state, viewModel)
                LayoutMode.GRID -> GridView(state, viewModel)
                LayoutMode.LIST -> ListView(state, viewModel)
            }
            Text("⚙", Modifier.align(Alignment.TopEnd).padding(10.dp).clickable { viewModel.openSettings(true) },
                color = MaterialTheme.colorScheme.primary)
            if (state.loading && state.entries.isEmpty()) Text(stringResource(R.string.loading), Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun GridView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val rows = state.entries.chunked(state.config.layout.gridColumns)
    LazyColumn(Modifier.fillMaxSize().padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(rows, key = { it.firstOrNull()?.key ?: it.hashCode() }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { entry -> AppChip(entry, state, viewModel, state.config.layout.showLabels) }
            }
        }
    }
}

@Composable
private fun ListView(state: LauncherUiState, viewModel: LauncherViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(top = 34.dp, start = 28.dp, end = 28.dp)) {
        items(state.entries, key = { it.key }) { entry ->
            Row(Modifier.fillMaxWidth().clickable { viewModel.launch(entry) }.padding(18.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppGlyph(entry, state, Modifier.size(42.dp), state.icons[entry.key])
                Text(entry.label.take(14), Modifier.padding(start = 10.dp), color = Color.White, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AppChip(entry: com.n2.oemlauncher.shared.model.CatalogEntry, state: LauncherUiState, viewModel: LauncherViewModel, showLabel: Boolean) {
    Column(Modifier.clickable { viewModel.launch(entry) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppGlyph(entry, state, Modifier.size(48.dp), state.icons[entry.key])
        if (showLabel) Text(entry.label, Modifier.width(110.dp).padding(top = 4.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
