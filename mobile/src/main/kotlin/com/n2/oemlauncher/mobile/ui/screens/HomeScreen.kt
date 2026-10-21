package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.ChampagneButton
import com.n2.oemlauncher.mobile.ui.components.GlassButton
import com.n2.oemlauncher.mobile.ui.components.GlassCard

@Composable
fun HomeScreen(state: CompanionState, vm: CompanionViewModel, navigate: (HomeDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        if (state.catalog == null) item { EmptyHero(vm) } else item {
            HeroCard(state, vm) { navigate(HomeDestination.PREVIEW) }
        }
        if (state.conflict) item { ConflictCard(state, vm) }
        item { EntryTiles(enabled = state.catalog != null, navigate = navigate) }
        item { Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
    }
}

enum class HomeDestination { EDITOR, APPEARANCE, ICONS, PREVIEW, SYNC, HELP }

@Composable
private fun ConflictCard(state: CompanionState, vm: CompanionViewModel) {
    GlassCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
            Text(stringResource(R.string.sync_conflict), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.conflict_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChampagneButton(
                stringResource(R.string.overwrite_watch),
                { vm.sync(overwrite = true) },
                Modifier.fillMaxWidth(),
                enabled = !state.busy,
            )
            GlassButton(
                stringResource(R.string.load_watch),
                { vm.loadWatch() },
                Modifier.fillMaxWidth(),
                enabled = !state.busy,
            )
        }
    }
}

