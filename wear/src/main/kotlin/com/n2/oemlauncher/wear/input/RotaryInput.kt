package com.n2.oemlauncher.wear.input

import androidx.compose.ui.Modifier
/** Wear rotary is consumed by the scroll containers; this hook keeps haptics injectable. */
fun Modifier.launcherRotaryInput(onStep: () -> Unit): Modifier = this
