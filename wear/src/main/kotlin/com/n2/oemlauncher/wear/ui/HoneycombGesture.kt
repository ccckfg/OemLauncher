package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.HoneycombProjector
import com.n2.oemlauncher.shared.layout.PanBounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

const val LONG_PRESS_MILLIS = 500L
private const val INERTIA_STOP = 8f

/**
 * Drag, fling and tap handling for the honeycomb. Hit testing is nearest-center over the cells
 * that are still visible enough to tap. [onSettled] runs after every drag with the snapped cell,
 * or -1 when snapping is off.
 */
suspend fun PointerInputScope.honeycombPointerInput(
    geometry: HoneycombGeometry,
    projector: HoneycombProjector,
    frame: HoneycombFrame,
    bounds: PanBounds,
    iconRadius: Float,
    fitRadius: Float,
    panScratch: FloatArray,
    getPanX: () -> Float,
    getPanY: () -> Float,
    setPan: (Float, Float) -> Unit,
    snapToCenter: Boolean,
    reduceMotion: Boolean,
    onDragStart: () -> Unit,
    onSettled: (Int) -> Unit,
    onTap: (index: Int, centerX: Float, centerY: Float) -> Unit,
    onLongPress: (index: Int) -> Unit,
) {
    val touchSlop = viewConfiguration.touchSlop
    val minTouchRadius = HoneycombDefaults.MIN_TOUCH_DIAMETER_DP.dp.toPx() / 2f
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
                            projector.project(currentX, currentY, fitRadius, frame)
                            val hit = honeycombHitIndex(
                                frame,
                                down.position.x - size.width / 2f,
                                down.position.y - size.height / 2f,
                                iconRadius,
                                iconRadius.coerceAtLeast(minTouchRadius),
                            )
                            val hitX = if (hit >= 0) size.width / 2f + frame.x[hit] else 0f
                            val hitY = if (hit >= 0) size.height / 2f + frame.y[hit] else 0f
                            if (duration >= LONG_PRESS_MILLIS) onLongPress(hit) else onTap(hit, hitX, hitY)
                        } else if (reduceMotion) {
                            val settled = if (snapToCenter) {
                                geometry.snapTarget(currentX, currentY, panScratch)
                            } else {
                                bounds.clamp(currentX, currentY, panScratch)
                                -1
                            }
                            // Pan is committed before the callback so listeners see the final position.
                            setPan(panScratch[0], panScratch[1])
                            onSettled(settled)
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
                                val settled = if (snapToCenter) {
                                    geometry.snapTarget(x, y, panScratch)
                                } else {
                                    bounds.clamp(x, y, panScratch)
                                    -1
                                }
                                setPan(panScratch[0], panScratch[1])
                                onSettled(settled)
                            }
                        }
                        break
                    }
                    val dx = change.position.x - down.position.x
                    val dy = change.position.y - down.position.y
                    if (!dragged && dx * dx + dy * dy > touchSlop * touchSlop) {
                        dragged = true
                        onDragStart()
                    }
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
