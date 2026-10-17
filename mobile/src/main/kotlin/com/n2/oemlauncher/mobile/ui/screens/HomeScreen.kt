package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.data.ConnectionKind
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.ActionChip
import com.n2.oemlauncher.mobile.ui.components.ChampagneButton
import com.n2.oemlauncher.mobile.ui.components.EmptyState
import com.n2.oemlauncher.mobile.ui.components.GlassButton
import com.n2.oemlauncher.mobile.ui.components.GlassCard
import com.n2.oemlauncher.mobile.ui.components.GlassRow
import com.n2.oemlauncher.mobile.ui.components.StatusChip
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.mobile.ui.components.relativeTime
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme

@Composable
fun HomeScreen(state: CompanionState, vm: CompanionViewModel, navigate: (HomeDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        if (state.catalog == null) item {
            EmptyState(
                message = stringResource(R.string.empty_detail),
                action = stringResource(R.string.try_demo),
                onAction = { vm.demo() },
            )
        } else item {
            HeroCard(state, vm) { navigate(HomeDestination.PREVIEW) }
        }
        if (state.demo) item { DemoStrip() }
        if (state.conflict) item { ConflictCard(state, vm) }
        item { EntryTiles(enabled = state.catalog != null, navigate = navigate) }
        item { Spacer(Modifier.padding(UiDimensions.SMALL_GAP.dp)) }
    }
}

enum class HomeDestination { EDITOR, APPEARANCE, ICONS, PREVIEW, SYNC, HELP }

/** The one big glass card: the watch itself, its connection chip and the sync chip. */
@Composable
private fun HeroCard(state: CompanionState, vm: CompanionViewModel, open: () -> Unit) {
    val palette = GlassTheme.palette
    val connected = state.connection.kind == ConnectionKind.CONNECTED
    val node = state.connection.nodes.firstOrNull { it.hasWatchApp }?.name
    val status = when (state.connection.kind) {
        ConnectionKind.CONNECTED -> stringResource(R.string.connected)
        ConnectionKind.APP_MISSING -> stringResource(R.string.app_missing)
        ConnectionKind.PLAY_SERVICES_MISSING -> stringResource(R.string.play_missing)
        ConnectionKind.UNDISCOVERED -> stringResource(R.string.no_watch)
    }
    val chip = if (connected && node != null) {
        stringResource(R.string.connection_chip, status, node)
    } else {
        status
    }
    val detail = when {
        state.connection.kind == ConnectionKind.APP_MISSING -> R.string.app_missing_detail
        state.connection.kind == ConnectionKind.PLAY_SERVICES_MISSING -> R.string.play_missing_detail
        state.connection.kind == ConnectionKind.UNDISCOVERED -> R.string.no_watch_detail
        state.snapshot.lastSyncMillis > 0L -> null
        else -> R.string.connected_detail
    }
    val caption = if (state.snapshot.lastSyncMillis > 0L) {
        stringResource(R.string.last_sync, relativeTime(state.snapshot.lastSyncMillis))
    } else {
        detail?.let { stringResource(it) }.orEmpty()
    }
    GlassCard(
        elevated = true,
        padding = PaddingValues(
            start = GlassTokens.CARD_PADDING.dp,
            end = GlassTokens.CARD_PADDING.dp,
            top = 20.dp,
            bottom = GlassTokens.CARD_PADDING.dp,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size((GlassTokens.HERO_PREVIEW + HALO_MARGIN).dp).drawWithCache {
                        val halo = Brush.radialGradient(
                            listOf(
                                palette.accent.copy(alpha = GlassTokens.HERO_HALO_ALPHA),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension / 2f,
                        )
                        onDrawBehind { drawCircle(halo) }
                    },
                )
                WatchPreview(state, vm, diameterDp = GlassTokens.HERO_PREVIEW, onOpen = open)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CHIP_GAP.dp)) {
                StatusChip(chip, dot = if (connected) palette.positive else null)
                if (state.canSync) {
                    ActionChip(stringResource(R.string.sync_changes), { vm.sync() })
                } else if (!state.demo) {
                    ActionChip(stringResource(R.string.refresh), { vm.refresh() }, icon = Icons.Outlined.Refresh)
                }
            }
            Text(
                stringResource(state.phase.label()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (caption.isNotEmpty()) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DemoStrip() {
    GlassRow {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.demo), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.demo_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = UiDimensions.DETAIL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

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

private const val HALO_MARGIN = 24
private const val CHIP_GAP = 10
