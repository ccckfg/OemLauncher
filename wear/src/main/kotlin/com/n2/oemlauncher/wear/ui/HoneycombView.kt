package com.n2.oemlauncher.wear.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.layout.FisheyeLens
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.PanBounds
import com.n2.oemlauncher.wear.input.HapticController
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

private const val LONG_PRESS_MILLIS = 500L
private const val INERTIA_STOP = 8f
private val favoriteStroke = androidx.compose.ui.graphics.drawscope.Stroke(2f)

@Composable
fun HoneycombView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val haptics = remember { HapticController(context) }
    val iconDiameter = with(density) { HoneycombDefaults.iconDiameterDp(state.config.layout.iconSize).dp.toPx() }
    val geometry = remember(state.entries.size, iconDiameter) { HoneycombGeometry(state.entries.size, HoneycombDefaults.pitchFor(iconDiameter)) }
    val lens = remember(state.config.layout.fisheyeStrength) { FisheyeLens(state.config.layout.fisheyeStrength) }
    val projector = remember(geometry, lens, iconDiameter) { com.n2.oemlauncher.shared.layout.HoneycombProjector(geometry, lens, iconDiameter / 2f) }
    val frame = remember(geometry.count) { HoneycombFrame(geometry.count) }
    val bounds = remember(geometry) { PanBounds.forGeometry(geometry, 227f) }
    val imageIcons = remember(state.icons) { state.icons.mapValues { it.value.asImageBitmap() } }
    var panX by remember(geometry) { mutableFloatStateOf(0f) }
    var panY by remember(geometry) { mutableFloatStateOf(0f) }
    val centerIndex = remember { mutableIntStateOf(0) }
    var lastRotaryTicks by remember { mutableIntStateOf(state.rotaryTicks) }
    var rotaryRemainder by remember { mutableFloatStateOf(0f) }
    val panScratch = remember { FloatArray(2) }

    LaunchedEffect(state.entries.size, state.centerRequest) {
        centerIndex.intValue = 0
        panX = 0f
        panY = 0f
    }
    LaunchedEffect(state.rotaryTicks, geometry.count) {
        val delta = state.rotaryTicks - lastRotaryTicks
        if (delta != 0 && geometry.count > 0) {
            val direction = if (delta > 0) 1 else -1
            repeat(kotlin.math.abs(delta)) {
                centerIndex.intValue = (centerIndex.intValue + direction + geometry.count) % geometry.count
                geometry.panToCenter(centerIndex.intValue, panScratch)
                panX = panScratch[0]
                panY = panScratch[1]
                haptics.tick(state.config.layout.hapticsEnabled)
            }
        }
        lastRotaryTicks = state.rotaryTicks
    }
    LaunchedEffect(state.entries.size) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }

    Box(
        Modifier.fillMaxSize().onRotaryScrollEvent { event ->
            rotaryRemainder += event.verticalScrollPixels
            var steps = rotaryRemainder.toInt()
            rotaryRemainder -= steps
            while (steps != 0 && geometry.count > 0) {
                val direction = if (steps > 0) 1 else -1
                steps -= direction
                centerIndex.intValue = (centerIndex.intValue + direction + geometry.count) % geometry.count
                geometry.panToCenter(centerIndex.intValue, panScratch)
                panX = panScratch[0]
                panY = panScratch[1]
                haptics.tick(state.config.layout.hapticsEnabled)
            }
            true
        }.focusRequester(focusRequester).focusable(),
    ) {
        Canvas(Modifier.fillMaxSize().semantics {
            contentDescription = state.entries.getOrNull(centerIndex.intValue)?.label ?: "App grid"
        }.pointerInput(geometry, state.config.layout.reduceMotion, state.config.layout.snapToCenter) {
            honeycombPointerInput(
                geometry = geometry,
                projector = projector,
                frame = frame,
                bounds = bounds,
                iconRadius = iconDiameter / 2f,
                panScratch = panScratch,
                getPanX = { panX },
                getPanY = { panY },
                setPan = { x, y -> panX = x; panY = y },
                snapToCenter = state.config.layout.snapToCenter,
                reduceMotion = state.config.layout.reduceMotion,
                onSettled = { centerIndex.intValue = it },
                onTap = { index, x, y ->
                    if (index >= 0) {
                        centerIndex.intValue = index
                        viewModel.launch(state.entries[index], RectF(x - iconDiameter / 2f, y - iconDiameter / 2f, x + iconDiameter / 2f, y + iconDiameter / 2f))
                    }
                },
                onLongPress = { index -> if (index >= 0) viewModel.showMenu(state.entries[index].key) },
            )
        }) {
            val radius = max(size.minDimension / 2f - 12f, 1f)
            projector.project(panX, panY, radius, frame)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            for (index in 0 until frame.size) {
                val alpha = frame.alpha[index]
                if (alpha <= 0f) continue
                val entry = state.entries[index]
                val x = centerX + frame.x[index]
                val y = centerY + frame.y[index]
                val r = iconDiameter / 2f * frame.scale[index]
                drawCircle(Color(0xFF14263A), r, Offset(x, y), alpha = alpha)
                imageIcons[entry.key]?.let { drawScaledIcon(it, x, y, r) }
                if (entry.key in state.config.favorites) {
                    drawCircle(Color(0xFF64D8FF), r + 2f, Offset(x, y), alpha = alpha, style = favoriteStroke)
                }
            }
        }
        if (state.menuKey == null) {
            Box(Modifier.fillMaxWidth().height(110.dp).align(androidx.compose.ui.Alignment.BottomCenter).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))),
            ))
            state.entries.getOrNull(centerIndex.intValue)?.let { entry ->
                Text(entry.label, Modifier.align(androidx.compose.ui.Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp).padding(bottom = 14.dp), color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScaledIcon(icon: ImageBitmap, x: Float, y: Float, radius: Float) {
    val diameter = (radius * 2f).toInt().coerceAtLeast(1)
    drawImage(icon, dstOffset = IntOffset((x - radius).toInt(), (y - radius).toInt()), dstSize = IntSize(diameter, diameter))
}

private suspend fun PointerInputScope.honeycombPointerInput(
    geometry: HoneycombGeometry,
    projector: com.n2.oemlauncher.shared.layout.HoneycombProjector,
    frame: HoneycombFrame,
    bounds: PanBounds,
    iconRadius: Float,
    panScratch: FloatArray,
    getPanX: () -> Float,
    getPanY: () -> Float,
    setPan: (Float, Float) -> Unit,
    snapToCenter: Boolean,
    reduceMotion: Boolean,
    onSettled: (Int) -> Unit,
    onTap: (index: Int, centerX: Float, centerY: Float) -> Unit,
    onLongPress: (index: Int) -> Unit,
) {
    val touchSlop = viewConfiguration.touchSlop
    var inertia: Job? = null
    coroutineScope {
        awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            inertia?.cancel()
            var dragged = false
            var lastX = down.position.x
            var lastY = down.position.y
            var velocityX = 0f
            var velocityY = 0f
            val startTime = down.uptimeMillis
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull() ?: break
                if (change.changedToUp()) {
                    val duration = change.uptimeMillis - startTime
                    val currentX = getPanX()
                    val currentY = getPanY()
                    if (!dragged) {
                        projector.project(currentX, currentY, minOf(size.width, size.height) / 2f - 12f, frame)
                        val hit = honeycombHitIndex(frame, down.position.x - size.width / 2f, down.position.y - size.height / 2f, iconRadius, iconRadius.coerceAtLeast(24f))
                        val hitX = if (hit >= 0) size.width / 2f + frame.x[hit] else 0f
                        val hitY = if (hit >= 0) size.height / 2f + frame.y[hit] else 0f
                        if (duration >= LONG_PRESS_MILLIS) onLongPress(hit) else onTap(hit, hitX, hitY)
                    } else if (reduceMotion) {
                        if (snapToCenter) geometry.snapTarget(currentX, currentY, panScratch).also { onSettled(it) } else {
                            bounds.clamp(currentX, currentY, panScratch)
                        }
                        setPan(panScratch[0], panScratch[1])
                    } else {
                        inertia = this@coroutineScope.launch {
                            var x = currentX
                            var y = currentY
                            var vx = velocityX
                            var vy = velocityY
                            while (abs(vx) + abs(vy) > INERTIA_STOP) {
                                withFrameNanos {
                                    x += vx * 0.016f
                                    y += vy * 0.016f
                                    bounds.rubberBand(x, y, panScratch)
                                    setPan(panScratch[0], panScratch[1])
                                    vx *= 0.90f
                                    vy *= 0.90f
                                }
                            }
                            val settled = if (snapToCenter) geometry.snapTarget(x, y, panScratch) else {
                                bounds.clamp(x, y, panScratch)
                                -1
                            }
                            if (settled >= 0) onSettled(settled)
                            setPan(panScratch[0], panScratch[1])
                        }
                    }
                    break
                }
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (!dragged && dx * dx + dy * dy > touchSlop * touchSlop) dragged = true
                if (dragged) {
                    val deltaX = change.position.x - lastX
                    val deltaY = change.position.y - lastY
                    val x = getPanX()
                    val y = getPanY()
                    bounds.rubberBand(x + deltaX, y + deltaY, panScratch)
                    setPan(panScratch[0], panScratch[1])
                    velocityX = deltaX * 60f
                    velocityY = deltaY * 60f
                    change.consume()
                }
                lastX = change.position.x
                lastY = change.position.y
            }
        }
        }
    }
}
