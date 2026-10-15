package com.n2.oemlauncher.wear.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.shared.layout.FisheyeLens
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.HoneycombProjector
import com.n2.oemlauncher.shared.layout.PanBounds
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.input.HapticController
import kotlin.math.max
import kotlin.math.min

/** Height of the bottom band (focus label + settings entry) and the fade above it, in dp. */
private val BAND_HEIGHT = 96.dp
private val BAND_FADE = 24.dp
private val favoriteStroke = Stroke(2f)

/**
 * Icons fade to nothing before the bottom band starts, so the settings entry and the focus
 * label never cover a visible icon no matter where the honeycomb is dragged.
 */
internal fun bandFade(y: Float, bandTop: Float, fadeMargin: Float): Float =
    if (fadeMargin <= 0f) 1f else ((bandTop - y) / fadeMargin).coerceIn(0f, 1f)

@Composable
fun HoneycombView(state: LauncherUiState, viewModel: LauncherViewModel) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val haptics = remember { HapticController(context) }
    val menuLabel = stringResource(R.string.app_menu)
    val iconDiameter = with(density) { HoneycombDefaults.iconDiameterDp(state.config.layout.iconSize).dp.toPx() }
    val geometry = remember(state.entries.size, iconDiameter) {
        HoneycombGeometry(state.entries.size, HoneycombDefaults.pitchFor(iconDiameter))
    }
    val lens = remember(state.config.layout.fisheyeStrength) { FisheyeLens(state.config.layout.fisheyeStrength) }
    val projector = remember(geometry, lens, iconDiameter) { HoneycombProjector(geometry, lens, iconDiameter / 2f) }
    val frame = remember(geometry.count) { HoneycombFrame(geometry.count) }
    val bounds = remember(geometry) { PanBounds.forGeometry(geometry, 227f) }
    val imageIcons = remember(state.icons) { state.icons.mapValues { it.value.asImageBitmap() } }
    var panX by remember(geometry) { mutableFloatStateOf(0f) }
    var panY by remember(geometry) { mutableFloatStateOf(0f) }
    val centerIndex = remember { mutableIntStateOf(0) }
    var lastRotaryTicks by remember { mutableIntStateOf(state.rotaryTicks) }
    var rotaryRemainder by remember { mutableFloatStateOf(0f) }
    val panScratch = remember { FloatArray(2) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var a11yNodes by remember(geometry) { mutableStateOf(emptyList<A11yNode>()) }
    val bandHeightPx = with(density) { BAND_HEIGHT.toPx() }
    val bandFadePx = with(density) { BAND_FADE.toPx() }

    // Accessibility proxies are rebuilt only when the honeycomb settles or steps, never per
    // frame, so dragging stays allocation-free.
    fun refreshA11y() {
        val size = canvasSize
        if (size.width == 0 || geometry.count == 0) return
        projector.project(panX, panY, min(size.width, size.height) / 2f - 12f, frame)
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val bandTop = size.height - bandHeightPx
        a11yNodes = (0 until frame.size).mapNotNull { index ->
            val y = centerY + frame.y[index]
            val alpha = frame.alpha[index] * bandFade(y, bandTop, bandFadePx)
            if (alpha < HoneycombDefaults.MIN_TAPPABLE_ALPHA) {
                null
            } else {
                A11yNode(index, centerX + frame.x[index], y, iconDiameter * frame.scale[index])
            }
        }
    }

    LaunchedEffect(state.entries.size, state.centerRequest) {
        centerIndex.intValue = 0
        panX = 0f
        panY = 0f
        refreshA11y()
    }
    LaunchedEffect(canvasSize, geometry.count) { refreshA11y() }
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
            refreshA11y()
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
            refreshA11y()
            true
        }.focusRequester(focusRequester).focusable(),
    ) {
        Canvas(
            Modifier.fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .semantics {
                    contentDescription = state.entries.getOrNull(centerIndex.intValue)?.label ?: "App grid"
                }
                .pointerInput(geometry, state.config.layout.reduceMotion, state.config.layout.snapToCenter) {
                    honeycombPointerInput(
                        geometry = geometry,
                        projector = projector,
                        frame = frame,
                        bounds = bounds,
                        iconRadius = iconDiameter / 2f,
                        panScratch = panScratch,
                        // PointerInputScope is a Density, so dp converts here; size is the canvas size.
                        bandTopPx = size.height - BAND_HEIGHT.toPx(),
                        bandFadePx = BAND_FADE.toPx(),
                        getPanX = { panX },
                        getPanY = { panY },
                        setPan = { x, y -> panX = x; panY = y },
                        snapToCenter = state.config.layout.snapToCenter,
                        reduceMotion = state.config.layout.reduceMotion,
                        onDragStart = { a11yNodes = emptyList() },
                        onSettled = { settled ->
                            centerIndex.intValue = settled
                            refreshA11y()
                        },
                        onTap = { index, x, y ->
                            if (index >= 0) {
                                centerIndex.intValue = index
                                viewModel.launch(
                                    state.entries[index],
                                    RectF(x - iconDiameter / 2f, y - iconDiameter / 2f, x + iconDiameter / 2f, y + iconDiameter / 2f),
                                )
                            }
                        },
                        onLongPress = { index -> if (index >= 0) viewModel.showMenu(state.entries[index].key) },
                    )
                },
        ) {
            val radius = max(size.minDimension / 2f - 12f, 1f)
            projector.project(panX, panY, radius, frame)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val bandTop = size.height - bandHeightPx
            for (index in 0 until frame.size) {
                val x = centerX + frame.x[index]
                val y = centerY + frame.y[index]
                val alpha = frame.alpha[index] * bandFade(y, bandTop, bandFadePx)
                if (alpha <= 0f) continue
                val entry = state.entries[index]
                val r = iconDiameter / 2f * frame.scale[index]
                drawCircle(Color(0xFF14263A), r, Offset(x, y), alpha = alpha)
                imageIcons[entry.key]?.let { drawScaledIcon(it, x, y, r) }
                if (entry.key in state.config.favorites) {
                    drawCircle(Color(0xFF64D8FF), r + 2f, Offset(x, y), alpha = alpha, style = favoriteStroke)
                }
            }
        }
        // Drawn after the canvas: Compose drops semantics nodes that a later sibling fully covers.
        if (state.menuKey == null) {
            HoneycombA11yOverlay(
                entries = state.entries,
                nodes = a11yNodes,
                menuLabel = menuLabel,
                onLaunch = { index -> viewModel.launch(state.entries[index], null) },
                onMenu = { index -> viewModel.showMenu(state.entries[index].key) },
            )
            Box(
                Modifier.fillMaxWidth().height(110.dp).align(Alignment.BottomCenter).background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))),
                ),
            )
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.entries.getOrNull(centerIndex.intValue)?.let { entry ->
                    Text(
                        entry.label,
                        Modifier.background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(8.dp))
                SettingsEntry(onClick = { viewModel.openSettings(true) })
            }
        }
    }
}

private fun DrawScope.drawScaledIcon(icon: ImageBitmap, x: Float, y: Float, radius: Float) {
    val diameter = (radius * 2f).toInt().coerceAtLeast(1)
    drawImage(icon, dstOffset = IntOffset((x - radius).toInt(), (y - radius).toInt()), dstSize = IntSize(diameter, diameter))
}
