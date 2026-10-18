package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.EditableAppRow
import com.n2.oemlauncher.mobile.ui.components.EmptyState
import com.n2.oemlauncher.mobile.ui.components.GlassButton
import com.n2.oemlauncher.mobile.ui.components.GlassSegmented
import com.n2.oemlauncher.mobile.ui.components.GroupLabel
import com.n2.oemlauncher.mobile.ui.components.SearchPill
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.ordering.AppOrdering

@Composable
fun EditorScreen(state: CompanionState, vm: CompanionViewModel, chooseIcon: (String) -> Unit) {
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
        val previewEdge = (maxHeight.value - GlassTokens.PREVIEW_FOOTER).toInt()
            .coerceIn(GlassTokens.PREVIEW_MIN, GlassTokens.PREVIEW_MAX)
        Row {
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = UiDimensions.PAGE_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
            ) {
                item { SearchPill(query) { query = it } }
                item {
                    GlassSegmented(
                        options = SortMode.entries,
                        selected = state.config.sortMode,
                        label = { stringResource(it.label()) },
                        onSelect = { vm.sort(it) },
                        description = stringResource(R.string.sort_by),
                    )
                }
                item { GroupLabel(stringResource(R.string.favorites, arranged.favorites.size)) }
                itemsIndexed(favorites, key = { _, app -> "favorite-${app.key}" }) { _, app ->
                    val index = arranged.favorites.indexOf(app)
                    EditableAppRow(
                        app, state.config, vm, index > 0, index < arranged.favorites.lastIndex,
                        { vm.favorite(app.key) }, { vm.hide(app.key, true) }, { chooseIcon(app.key) },
                        { delta -> vm.moveFavoriteKey(app.key, delta) },
                        if (state.config.layout.reduceMotion) Modifier else Modifier.animateItem(),
                    )
                }
                if (arranged.favorites.isEmpty()) item {
                    Text(
                        stringResource(R.string.favorite_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { GroupLabel(stringResource(R.string.all_apps, arranged.others.size)) }
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
                    EmptyState(
                        message = stringResource(R.string.no_results),
                        action = stringResource(R.string.clear_search),
                        onAction = { query = "" },
                    )
                }
                item {
                    GlassButton(
                        stringResource(R.string.hidden_apps, hidden.size),
                        { showHidden = !showHidden },
                        Modifier.fillMaxWidth(),
                    )
                }
                if (showHidden) itemsIndexed(hidden, key = { _, app -> "hidden-${app.key}" }) { _, app ->
                    EditableAppRow(
                        app, state.config, vm, false, false,
                        { vm.favorite(app.key) }, { vm.hide(app.key, false) }, { chooseIcon(app.key) }, {},
                    )
                }
                item {
                    Text(
                        stringResource(R.string.saved_automatically),
                        Modifier.padding(start = 4.dp, top = UiDimensions.SMALL_GAP.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
            }
            if (wide) Column(
                Modifier.width(UiDimensions.PREVIEW_PANE.dp).padding(UiDimensions.GAP.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                WatchPreview(state, vm, maxEdgeDp = previewEdge)
            }
        }
    }
}
