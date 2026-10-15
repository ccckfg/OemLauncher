package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.ChoiceRow
import com.n2.oemlauncher.mobile.ui.components.EditableAppRow
import com.n2.oemlauncher.mobile.ui.components.SearchField
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.ordering.AppOrdering

@Composable
fun EditorScreen(state: CompanionState, vm: CompanionViewModel, chooseIcon: (String) -> Unit, preview: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    val arranged = remember(state.catalog, state.config) {
        AppOrdering.arrange(state.catalog?.entries.orEmpty(), state.config)
    }
    val search by vm.searchIndex.collectAsStateWithLifecycle()
    val favorites = remember(arranged, query, search) { arranged.favorites.filter { search.matches(it, query) } }
    val others = remember(arranged, query, search) { arranged.others.filter { search.matches(it, query) } }
    val hidden = remember(state.catalog, state.config.hidden, query, search) {
        state.catalog?.entries.orEmpty().filter { it.key in state.config.hidden && search.matches(it, query) }
    }
    BoxWithConstraints {
        val wide = maxWidth >= UiDimensions.WIDE_BREAKPOINT.dp
        val previewEdge = (maxHeight.value - UiDimensions.PREVIEW_FOOTER).toInt()
            .coerceIn(UiDimensions.THUMBNAIL, UiDimensions.PREVIEW)
        Row {
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = UiDimensions.PAGE_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(preview) { Text(stringResource(R.string.preview)) }
                    }
                    Text(stringResource(R.string.saved_automatically), style = MaterialTheme.typography.bodySmall)
                }
                if (!wide) item { WatchPreview(state, vm, thumbnail = true, onOpen = preview) }
                item { SearchField(query) { query = it } }
                item {
                    ChoiceRow(SortMode.entries, state.config.sortMode, { it.label() }) { vm.sort(it) }
                }
                item {
                    Text(stringResource(R.string.favorites, arranged.favorites.size), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.favorites_hint), style = MaterialTheme.typography.bodySmall)
                    if (arranged.favorites.isEmpty()) Text(stringResource(R.string.favorite_empty))
                }
                itemsIndexed(favorites, key = { _, app -> "favorite-${app.key}" }) { _, app ->
                    val index = arranged.favorites.indexOf(app)
                    EditableAppRow(
                        app, state.config, vm, index > 0, index < arranged.favorites.lastIndex,
                        { vm.favorite(app.key) }, { vm.hide(app.key, true) }, { chooseIcon(app.key) },
                        { delta -> vm.moveFavoriteKey(app.key, delta) },
                        if (state.config.layout.reduceMotion) Modifier else Modifier.animateItem(),
                    )
                }
                item { Text(stringResource(R.string.all_apps, arranged.others.size), style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(others, key = { _, app -> app.key }) { _, app ->
                    val index = arranged.others.indexOf(app)
                    EditableAppRow(
                        app, state.config, vm, index > 0, index < arranged.others.lastIndex,
                        { vm.favorite(app.key) }, { vm.hide(app.key, true) }, { chooseIcon(app.key) },
                        { vm.moveApp(app.key, it) },
                        if (state.config.layout.reduceMotion) Modifier else Modifier.animateItem(),
                    )
                }
                if (favorites.isEmpty() && others.isEmpty() && query.isNotBlank()) item {
                    Text(stringResource(R.string.no_results))
                }
                item {
                    TextButton({ showHidden = !showHidden }) { Text(stringResource(R.string.hidden_apps, hidden.size)) }
                }
                if (showHidden) itemsIndexed(hidden, key = { _, app -> "hidden-${app.key}" }) { _, app ->
                    EditableAppRow(
                        app, state.config, vm, false, false,
                        { vm.favorite(app.key) }, { vm.hide(app.key, false) }, { chooseIcon(app.key) }, {},
                    )
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.padding(UiDimensions.GAP.dp)) }
            }
            if (wide) Column(
                Modifier.width(UiDimensions.PREVIEW_PANE.dp).padding(UiDimensions.GAP.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                WatchPreview(state, vm, maxEdgeDp = previewEdge)
                Text(stringResource(R.string.preview_detail), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
