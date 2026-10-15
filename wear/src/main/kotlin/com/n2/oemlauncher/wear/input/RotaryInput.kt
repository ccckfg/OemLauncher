package com.n2.oemlauncher.wear.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.rotary.onRotaryScrollEvent

/** Shared rotary adapter for screens that need to map crown pixels to their own scroll model. */
fun Modifier.launcherRotaryInput(onScroll: (Float) -> Unit): Modifier =
    onRotaryScrollEvent { event ->
        onScroll(event.verticalScrollPixels)
        true
    }
