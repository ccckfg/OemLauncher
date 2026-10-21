package com.n2.oemlauncher.wear.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.wear.config.WearUiTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** How the launcher content enters, following docs/design/README.md section 8. */
enum class EntranceStyle(val durationMs: Int) {
    /** Refined honeycomb: rings bloom out of the center gear one after another. */
    BLOOM(WearUiTokens.MOTION_EMPHASIS_MS),

    /** Refined grid and list: the page entrance, a fade while growing from 0.96. */
    PAGE(WearUiTokens.MOTION_STANDARD_MS),

    /** Lite tier and reduced motion: a short plain fade, no scaling. */
    FADE(WearUiTokens.MOTION_FADE_MAX_MS),
    ;

    companion object {
        fun of(layout: LayoutSettings): EntranceStyle = when {
            layout.visualTier == VisualTier.LITE || layout.reduceMotion -> FADE
            layout.mode == LayoutMode.HONEYCOMB -> BLOOM
            else -> PAGE
        }
    }
}

internal object LauncherEntrance {
    /**
     * Eased progress of one honeycomb cell at linear timeline [progress]. [reach] is the cell's
     * distance from the center as a share of the fit radius: the center starts first and the rim
     * last, each cell easing over the same share of the timeline.
     */
    fun cellProgress(progress: Float, reach: Float): Float {
        if (progress >= 1f) return 1f
        val stagger = WearUiTokens.ENTRANCE_STAGGER
        val local = ((progress - reach.coerceIn(0f, 1f) * stagger) / (1f - stagger)).coerceIn(0f, 1f)
        return LinearOutSlowInEasing.transform(local)
    }

    fun cellScale(cellProgress: Float): Float = lerp(WearUiTokens.ENTRANCE_START_SCALE, cellProgress)

    fun cellSpread(cellProgress: Float): Float = lerp(WearUiTokens.ENTRANCE_START_SPREAD, cellProgress)

    fun pageScale(progress: Float): Float = lerp(WearUiTokens.PAGE_START_SCALE, LinearOutSlowInEasing.transform(progress))

    private fun lerp(start: Float, fraction: Float) = start + (1f - start) * fraction
}

/**
 * Linear 0→1 timeline of the entrance. It starts once the content is [ready] and the window is
 * [windowEntered]: on a reopen that is right after the window fade, on a cold start it waits for
 * the stored icons. A window that never reports its transition still gets the entrance after a
 * short wait, so content can never stay hidden. The played flag is saveable, so coming back from
 * settings does not replay it; every Home press creates a new activity and plays it again.
 */
@Composable
internal fun rememberEntranceProgress(ready: Boolean, windowEntered: Boolean, style: EntranceStyle): () -> Float {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    val entered by rememberUpdatedState(windowEntered)
    LaunchedEffect(ready) {
        if (ready && !played) {
            withTimeoutOrNull(WearUiTokens.ENTRANCE_WINDOW_WAIT_MS) { snapshotFlow { entered }.first { it } }
            played = true
            progress.animateTo(1f, tween(style.durationMs, easing = LinearEasing))
        }
    }
    return remember(progress) { { progress.value } }
}

/** Timeline of content that does not animate its entrance. */
internal val Settled: () -> Float = { 1f }

/** Page and fade entrances move the whole layer; the bloom is drawn per cell by the honeycomb. */
internal fun Modifier.entrance(style: EntranceStyle, progress: () -> Float): Modifier = when (style) {
    EntranceStyle.BLOOM -> this
    EntranceStyle.PAGE -> graphicsLayer {
        val value = progress()
        alpha = value
        scaleX = LauncherEntrance.pageScale(value)
        scaleY = scaleX
    }
    EntranceStyle.FADE -> graphicsLayer { alpha = progress() }
}
