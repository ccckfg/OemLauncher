package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        query, onChange, Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton({ onChange("") }) {
                Icon(Icons.Outlined.Clear, stringResource(R.string.clear_search))
            }
        },
    )
}

@Composable
fun AppIcon(app: CatalogEntry, config: LauncherConfig, vm: CompanionViewModel, iconId: String? = null) {
    val id = iconId ?: config.iconOverrides[app.key] ?: app.iconId
    val images by vm.iconImages.collectAsStateWithLifecycle()
    LaunchedEffect(id) { if (id != null) vm.prefetchIcons(listOf(id)) }
    val image = id?.let { images[it] }
    Surface(
        Modifier.size(UiDimensions.APP_ICON.dp), shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        if (image != null) {
            Image(
                image, contentDescription = null,
                modifier = Modifier.size(UiDimensions.APP_ICON.dp).clip(CircleShape),
            )
        } else {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(app.label.take(1), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> Int, onSelect: (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
    ) {
        values.forEach { item ->
            FilterChip(
                selected = item == selected, onClick = { onSelect(item) },
                label = { Text(stringResource(label(item)), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
fun Processing() {
    Column(
        Modifier.fillMaxWidth().padding(UiDimensions.PAGE_PADDING.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.processing))
    }
}
