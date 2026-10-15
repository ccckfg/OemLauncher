package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.input.HapticController
import kotlin.math.abs

/**
 * Crown scrolling for a column. MainActivity consumes rotary events before Compose sees them
 * (the honeycomb steps per tick), so scrolling screens follow the tick counter instead.
 * Ticks that happened before this screen appeared are ignored.
 */
@Composable
fun RotaryTicksScroll(listState: TransformingLazyColumnState, ticks: Int, haptics: Boolean) {
    val context = LocalContext.current
    val controller = remember { HapticController(context) }
    var last by remember { mutableIntStateOf(ticks) }
    LaunchedEffect(ticks) {
        val delta = ticks - last
        last = ticks
        if (delta != 0) {
            listState.scrollBy(delta * WearUiTokens.ROTARY_SCROLL_PX)
            repeat(abs(delta)) { controller.tick(haptics) }
        }
    }
}
