package com.n2.oemlauncher.mobile.ui.screens

import android.graphics.Path
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.data.ConnectionKind
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.components.ActionChip
import com.n2.oemlauncher.mobile.ui.components.ChampagneButton
import com.n2.oemlauncher.mobile.ui.components.GlassButton
import com.n2.oemlauncher.mobile.ui.components.GlassCard
import com.n2.oemlauncher.mobile.ui.components.StatusChip
import com.n2.oemlauncher.mobile.ui.components.WatchBackdrop
import com.n2.oemlauncher.mobile.ui.components.WatchPreview
import com.n2.oemlauncher.mobile.ui.components.label
import com.n2.oemlauncher.mobile.ui.components.relativeTime
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.design.CookieOutline
import com.n2.oemlauncher.shared.design.GlassSpecs
import com.n2.oemlauncher.shared.design.Palettes
import com.n2.oemlauncher.shared.model.VisualTier
import kotlin.math.abs

/** The one big glass card: the watch itself, one status line of chips and one caption. */
@Composable
fun HeroCard(state: CompanionState, vm: CompanionViewModel, open: () -> Unit) {
    val palette = GlassTheme.palette
    val connected = state.connection.kind == ConnectionKind.CONNECTED
    val node = state.connection.nodes.firstOrNull { it.hasWatchApp }?.name
    val status = when (state.connection.kind) {
        ConnectionKind.CONNECTED -> stringResource(R.string.connected)
        ConnectionKind.APP_MISSING -> stringResource(R.string.app_missing)
        ConnectionKind.PLAY_SERVICES_MISSING -> stringResource(R.string.play_missing)
        ConnectionKind.UNDISCOVERED -> stringResource(R.string.no_watch)
    }
    val chip = when {
        state.demo -> stringResource(R.string.demo)
        connected && node != null -> stringResource(R.string.connection_chip, status, node)
        else -> status
    }
    // One caption, never a stack: the demo explains itself, a missing watch says how to pair,
    // and a connected one reports where the last sync stands.
    val caption = when {
        state.demo -> stringResource(R.string.demo_detail)
        state.connection.kind == ConnectionKind.APP_MISSING -> stringResource(R.string.app_missing_detail)
        state.connection.kind == ConnectionKind.PLAY_SERVICES_MISSING -> stringResource(R.string.play_missing_detail)
        state.connection.kind == ConnectionKind.UNDISCOVERED -> stringResource(R.string.no_watch_detail)
        state.snapshot.lastSyncMillis > 0L -> stringResource(state.phase.label()) + CAPTION_SEPARATOR +
            stringResource(R.string.last_sync, relativeTime(state.snapshot.lastSyncMillis))
        else -> stringResource(state.phase.label()) + CAPTION_SEPARATOR + stringResource(R.string.connected_detail)
    }
    HeroFrame {
        Box(contentAlignment = Alignment.Center) {
            Halo(palette.accent)
            WatchPreview(state, vm, diameterDp = GlassTokens.HERO_PREVIEW, onOpen = open)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CHIP_GAP.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusChip(chip, dot = if (connected && !state.demo) palette.positive else null)
            if (state.canSync) {
                ActionChip(stringResource(R.string.sync_changes), { vm.sync() })
            } else if (!state.demo) {
                ActionChip(stringResource(R.string.refresh), { vm.refresh() }, icon = Icons.Outlined.Refresh)
            }
        }
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Before any catalog exists: an empty watch in the same card, and the two ways forward. */
@Composable
fun EmptyHero(vm: CompanionViewModel) {
    HeroFrame {
        Box(contentAlignment = Alignment.Center) {
            Halo(GlassTheme.palette.accent)
            GhostWatch(GlassTokens.HERO_PREVIEW)
        }
        Text(
            stringResource(R.string.empty_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
            GlassButton(stringResource(R.string.refresh), { vm.refresh() }, Modifier.weight(1f))
            ChampagneButton(stringResource(R.string.try_demo), { vm.demo() }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroFrame(content: @Composable () -> Unit) {
    GlassCard(
        elevated = true,
        padding = PaddingValues(
            start = GlassTokens.CARD_PADDING.dp,
            end = GlassTokens.CARD_PADDING.dp,
            top = HERO_TOP_PADDING.dp,
            bottom = HERO_BOTTOM_PADDING.dp,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
        ) { content() }
    }
}

@Composable
private fun Halo(accent: Color) {
    Box(
        Modifier.size((GlassTokens.HERO_PREVIEW + HALO_MARGIN).dp).drawWithCache {
            val halo = Brush.radialGradient(
                listOf(accent.copy(alpha = GlassTokens.HERO_HALO_ALPHA), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension / 2f,
            )
            onDrawBehind { drawCircle(halo) }
        },
    )
}

/**
 * The watch with nothing on it yet: the backdrop, faint empty cells around the champagne
 * cookie, and the bezel. The watch is always Mocha, whatever the phone theme.
 */
@Composable
private fun GhostWatch(diameterDp: Int) {
    val watch = Palettes.Mocha
    val rim = GlassTheme.palette.rimLight
    val shadow = Color.Black.copy(alpha = GlassSpecs.Phone.shadowAlpha)
    Spacer(
        Modifier.size(diameterDp.dp)
            .shadow(GHOST_SHADOW.dp, CircleShape, ambientColor = shadow, spotColor = shadow)
            .clip(CircleShape)
            .drawWithCache {
                val backdrop = WatchBackdrop(size.width, size.height, VisualTier.REFINED)
                val center = Offset(size.width / 2f, size.height / 2f)
                val cellRadius = size.minDimension * GHOST_CELL_FRACTION
                val step = cellRadius * GHOST_STEP
                val cell = Color(watch.glass)
                // Two rings of a real honeycomb (axial coordinates), fading outwards.
                val cells = buildList {
                    for (q in -GHOST_RINGS..GHOST_RINGS) for (r in -GHOST_RINGS..GHOST_RINGS) {
                        val ring = maxOf(abs(q), abs(r), abs(q + r))
                        if (ring == 0 || ring > GHOST_RINGS) continue
                        val offset = Offset(step * (q + r / 2f), step * SQRT3_HALF * r)
                        add(center + offset to cell.copy(alpha = cell.alpha / ring))
                    }
                }
                val points = CookieOutline.points(cellRadius * GHOST_COOKIE_SCALE)
                val cookie = Path().apply {
                    moveTo(points[0] + center.x, points[1] + center.y)
                    for (i in 2 until points.size step 2) lineTo(points[i] + center.x, points[i + 1] + center.y)
                    close()
                }.asComposePath()
                val champagne = Brush.verticalGradient(
                    listOf(Color(watch.accentLight), Color(watch.accentDeep)),
                    startY = center.y - cellRadius, endY = center.y + cellRadius,
                )
                val ring = GlassTokens.BEZEL_RING.dp.toPx()
                onDrawBehind {
                    drawIntoCanvas { backdrop.drawBackground(it.nativeCanvas) }
                    cells.forEach { (at, color) -> drawCircle(color, cellRadius, at) }
                    drawPath(cookie, champagne)
                    drawIntoCanvas { backdrop.drawVignette(it.nativeCanvas) }
                    drawCircle(rim, size.minDimension / 2f - ring / 2f, style = Stroke(ring))
                }
            },
    )
}

private const val HALO_MARGIN = 24
private const val CHIP_GAP = 10
private const val HERO_TOP_PADDING = 20
private const val HERO_BOTTOM_PADDING = 20
private const val CAPTION_SEPARATOR = " · "
private const val GHOST_SHADOW = 16
private const val GHOST_CELL_FRACTION = 0.085f
private const val GHOST_STEP = 2.35f
private const val GHOST_COOKIE_SCALE = 1.1f
private const val GHOST_RINGS = 2
private const val SQRT3_HALF = 0.8660254f
