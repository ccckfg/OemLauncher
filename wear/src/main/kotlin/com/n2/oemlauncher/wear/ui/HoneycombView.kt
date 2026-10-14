package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.layout.FisheyeLens
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.HoneycombHitTest
import com.n2.oemlauncher.shared.layout.HoneycombProjector
import com.n2.oemlauncher.shared.layout.PanBounds
import com.n2.oemlauncher.wear.R
import kotlin.math.max

@Composable
fun HoneycombView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val density = LocalDensity.current
    val iconDiameter = with(density) { HoneycombDefaults.iconDiameterDp(state.config.layout.iconSize).dp.toPx() }
    val geometry = remember(state.entries.size, iconDiameter) { HoneycombGeometry(state.entries.size, HoneycombDefaults.pitchFor(iconDiameter)) }
    val lens = remember(state.config.layout.fisheyeStrength) { FisheyeLens(state.config.layout.fisheyeStrength) }
    val projector = remember(geometry, lens, iconDiameter) { HoneycombProjector(geometry, lens, iconDiameter / 2f) }
    val frame = remember(geometry.count) { HoneycombFrame(geometry.count) }
    val bounds = remember(geometry) { PanBounds.forGeometry(geometry, 227f) }
    var pan by remember(geometry) { mutableStateOf(Offset.Zero) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    val centerIndex = remember { mutableIntStateOf(0) }

    LaunchedEffect(state.entries.size, state.centerRequest) { pan = Offset.Zero }
    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(geometry, state.config.layout.reduceMotion) {
                detectDragGestures(
                    onDragStart = { dragStart = pan },
                    onDrag = { change, amount ->
                        change.consume()
                        val candidate = dragStart + (pan - dragStart) + amount
                        val result = FloatArray(2)
                        bounds.rubberBand(candidate.x, candidate.y, result)
                        pan = Offset(result[0], result[1])
                    },
                    onDragEnd = {
                        val result = FloatArray(2)
                        if (state.config.layout.snapToCenter) {
                            centerIndex.intValue = geometry.snapTarget(pan.x, pan.y, result)
                            pan = Offset(result[0], result[1])
                        } else {
                            bounds.clamp(pan.x, pan.y, result)
                            pan = Offset(result[0], result[1])
                        }
                    },
                )
            },
        ) {
            val radius = max(size.minDimension / 2f - 12f, 1f)
            projector.project(pan.x, pan.y, radius, frame)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            for (index in 0 until frame.size) {
                if (frame.alpha[index] <= 0f) continue
                val entry = state.entries[index]
                val x = centerX + frame.x[index]
                val y = centerY + frame.y[index]
                val r = iconDiameter / 2f * frame.scale[index]
                drawCircle(Color(0xFF14263A).copy(alpha = frame.alpha[index]), r, Offset(x, y))
                state.icons[entry.key]?.let {
                    val diameter = (r * 2f).toInt().coerceAtLeast(1)
                    drawImage(it.asImageBitmap(), dstOffset = androidx.compose.ui.unit.IntOffset((x - r).toInt(), (y - r).toInt()), dstSize = IntSize(diameter, diameter))
                }
                if (entry.key in state.config.favorites) drawCircle(Color(0xFF64D8FF), r + 2f, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
        }
        state.entries.getOrNull(centerIndex.intValue)?.let { focusedEntry ->
            Box(
                Modifier.align(androidx.compose.ui.Alignment.Center).size(92.dp).combinedClickable(
                    onClick = { viewModel.launch(focusedEntry) },
                    onLongClick = { viewModel.showMenu(focusedEntry.key) },
                ),
            )
        }
        val focused = state.entries.getOrNull(centerIndex.intValue)
        focused?.let { entry ->
            Box(Modifier.fillMaxSize().align(androidx.compose.ui.Alignment.BottomCenter).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))),
            ))
            Text(entry.label, Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 18.dp), color = Color.White)
        }
        if (state.menuKey != null) AppMenuDialog(state, viewModel, state.menuKey)
    }
}
