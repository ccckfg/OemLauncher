package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.wear.input.HapticController
import kotlin.math.abs

/**
 * List layout on TransformingLazyColumn: items scale down and fade as they leave the center,
 * which also gives the list the expected round-screen edge treatment.
 */
@Composable
fun ListView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val listState = rememberTransformingLazyColumnState()
    val context = LocalContext.current
    val haptics = remember { HapticController(context) }
    var lastRotaryTicks by remember { mutableIntStateOf(state.rotaryTicks) }
    LaunchedEffect(state.rotaryTicks) {
        val delta = state.rotaryTicks - lastRotaryTicks
        if (delta != 0) {
            listState.dispatchRawDelta(delta * 48f)
            repeat(abs(delta)) { haptics.tick(state.config.layout.hapticsEnabled) }
        }
        lastRotaryTicks = state.rotaryTicks
    }
    TransformingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        // Top padding puts the first row's center on the screen center for the medium icon size,
        // which is what the transforming scale is measured from.
        contentPadding = PaddingValues(start = 30.dp, top = 84.dp, end = 30.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.entries, key = { it.key }) { entry ->
            val iconDp = HoneycombDefaults.iconDiameterDp(state.config.layout.iconSize).dp
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .combinedClickable(
                        onClick = { viewModel.launch(entry) },
                        onLongClick = { viewModel.showMenu(entry.key) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppGlyph(entry, state, Modifier.size(iconDp), state.icons[entry.key])
                if (state.config.layout.showLabels) {
                    Text(
                        entry.label,
                        Modifier.padding(start = 12.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Last item instead of a floating button: nothing can ever be covered while scrolling.
        item { SettingsRow(onClick = { viewModel.openSettings(true) }) }
    }
}
