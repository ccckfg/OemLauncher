package com.n2.oemlauncher.wear.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.shared.layout.FisheyeLens
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.layout.HoneycombFrame
import com.n2.oemlauncher.shared.layout.HoneycombGeometry
import com.n2.oemlauncher.shared.layout.HoneycombProjector
import com.n2.oemlauncher.shared.layout.HoneycombSlots
import com.n2.oemlauncher.shared.layout.PanBounds
import com.n2.oemlauncher.wear.R
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.input.HapticController
import com.n2.oemlauncher.wear.ui.theme.edgeVignette
import kotlin.math.abs
import kotlin.math.max

/**
 * Full-screen honeycomb. Cell 0 is the settings gear, so the launcher rests with settings in
 * the center (as on MeoWearLauncher) and the apps spiral around it, favorites first. Icons use
 * the whole round screen; an edge vignette lets them sink into the bezel, and the centered
 * cell's name is curved along the bottom edge instead of covering icons.
 */
@Composable
fun HoneycombView(state: LauncherUiState, viewModel: LauncherViewModel, callbacks: HomeCallbacks) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val haptics = remember { HapticController(context) }
    val settingsLabel = stringResource(R.string.settings)
    val menuLabel = stringResource(R.string.app_menu)
    val switchLabel = stringResource(R.string.switch_layout)
    val layout = state.config.layout
    val layoutFavorites = state.config.favorites
    val latestState by rememberUpdatedState(state)
    val latestCallbacks by rememberUpdatedState(callbacks)

    val iconDiameter = with(density) { HoneycombDefaults.iconDiameterDp(layout.iconSize).dp.toPx() }
    val gearRadius = iconDiameter / 2f
    val gear = rememberGearGlyph(gearRadius, layout.visualTier)
    val viewportRadius = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() } / 2f
    val fitRadius = max(viewportRadius - with(density) { WearUiTokens.HONEYCOMB_EDGE_INSET_DP.dp.toPx() }, 1f)
    val cells = HoneycombSlots.cellCount(state.entries.size)
    val geometry = remember(cells, iconDiameter) { HoneycombGeometry(cells, HoneycombDefaults.pitchFor(iconDiameter)) }
    val lens = remember(layout.fisheyeStrength) { FisheyeLens(layout.fisheyeStrength) }
    val projector = remember(geometry, lens, iconDiameter) { HoneycombProjector(geometry, lens, iconDiameter / 2f) }
    val frame = remember(geometry.count) { HoneycombFrame(geometry.count) }
    val bounds = remember(geometry, viewportRadius) { PanBounds.forGeometry(geometry, viewportRadius) }
    val imageIcons = remember(state.icons) { state.icons.mapValues { it.value.asImageBitmap() } }

    var panX by remember(geometry) { mutableFloatStateOf(0f) }
    var panY by remember(geometry) { mutableFloatStateOf(0f) }
    var centerCell by remember { mutableIntStateOf(HoneycombSlots.SETTINGS) }
    var dragging by remember { mutableStateOf(false) }
    var lastRotaryTicks by remember { mutableIntStateOf(state.rotaryTicks) }
    val panScratch = remember { FloatArray(2) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var a11yNodes by remember(geometry) { mutableStateOf(emptyList<A11yNode>()) }

    fun labelOf(cell: Int): String =
        if (HoneycombSlots.isSettings(cell)) settingsLabel
        else latestState.entries.getOrNull(HoneycombSlots.appAt(cell))?.label.orEmpty()

    fun activate(cell: Int, source: RectF?) {
        if (HoneycombSlots.isSettings(cell)) {
            latestCallbacks.openSettings()
        } else {
            latestState.entries.getOrNull(HoneycombSlots.appAt(cell))?.let { viewModel.launch(it, source) }
        }
    }

    fun secondary(cell: Int) {
        if (HoneycombSlots.isSettings(cell)) {
            latestCallbacks.openSwitcher()
        } else {
            latestState.entries.getOrNull(HoneycombSlots.appAt(cell))?.let { latestCallbacks.openMenu(it.key) }
        }
    }

    // Accessibility proxies are rebuilt only when the honeycomb settles or steps, never per
    // frame, so dragging stays allocation-free.
    fun refreshA11y() {
        val size = canvasSize
        if (size.width == 0 || geometry.count == 0) return
        projector.project(panX, panY, fitRadius, frame)
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        a11yNodes = (0 until frame.size).mapNotNull { cell ->
            if (frame.alpha[cell] < HoneycombDefaults.MIN_TAPPABLE_ALPHA) {
                null
            } else {
                A11yNode(cell, centerX + frame.x[cell], centerY + frame.y[cell], iconDiameter * frame.scale[cell])
            }
        }
    }

    fun step(direction: Int) {
        centerCell = (centerCell + direction + geometry.count) % geometry.count
        geometry.panToCenter(centerCell, panScratch)
        panX = panScratch[0]
        panY = panScratch[1]
        haptics.tick(latestState.config.layout.hapticsEnabled)
    }

    LaunchedEffect(state.entries.size, state.centerRequest) {
        centerCell = HoneycombSlots.SETTINGS
        panX = 0f
        panY = 0f
        refreshA11y()
    }
    LaunchedEffect(canvasSize, geometry.count) { refreshA11y() }
    LaunchedEffect(state.rotaryTicks, geometry.count) {
        val delta = state.rotaryTicks - lastRotaryTicks
        if (delta != 0 && geometry.count > 0) {
            repeat(abs(delta)) { step(if (delta > 0) 1 else -1) }
            refreshA11y()
        }
        lastRotaryTicks = state.rotaryTicks
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier.fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .edgeVignette(layout.visualTier)
                .semantics { contentDescription = labelOf(centerCell) }
                .pointerInput(geometry, projector, bounds, fitRadius, layout.reduceMotion, layout.snapToCenter) {
                    honeycombPointerInput(
                        geometry = geometry,
                        projector = projector,
                        frame = frame,
                        bounds = bounds,
                        iconRadius = iconDiameter / 2f,
                        fitRadius = fitRadius,
                        panScratch = panScratch,
                        getPanX = { panX },
                        getPanY = { panY },
                        setPan = { x, y -> panX = x; panY = y },
                        snapToCenter = layout.snapToCenter,
                        reduceMotion = layout.reduceMotion,
                        onDragStart = {
                            dragging = true
                            a11yNodes = emptyList()
                        },
                        onSettled = { settled ->
                            if (settled >= 0) centerCell = settled
                            dragging = false
                            refreshA11y()
                        },
                        onTap = { cell, x, y ->
                            if (cell >= 0) {
                                val r = iconDiameter / 2f
                                activate(cell, RectF(x - r, y - r, x + r, y + r))
                            }
                        },
                        onLongPress = { cell -> if (cell >= 0) secondary(cell) },
                    )
                },
        ) {
            projector.project(panX, panY, fitRadius, frame)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = iconDiameter / 2f
            for (cell in 0 until frame.size) {
                val alpha = frame.alpha[cell]
                if (alpha <= 0f) continue
                val x = centerX + frame.x[cell]
                val y = centerY + frame.y[cell]
                val radius = baseRadius * frame.scale[cell]
                if (HoneycombSlots.isSettings(cell)) {
                    drawGearCell(gear, x, y, radius, alpha, pressed = dragging)
                } else {
                    val entry = state.entries.getOrNull(HoneycombSlots.appAt(cell)) ?: continue
                    drawAppCell(
                        imageIcons[entry.key],
                        x,
                        y,
                        radius,
                        alpha,
                        favorite = entry.key in layoutFavorites,
                    )
                }
            }
        }
        HoneycombFocusLabel(labelOf(centerCell), visible = !dragging && layout.showLabels)
        HoneycombA11yOverlay(
            nodes = a11yNodes,
            labelOf = ::labelOf,
            actionLabelOf = { cell -> if (HoneycombSlots.isSettings(cell)) switchLabel else menuLabel },
            onActivate = { cell -> activate(cell, null) },
            onAction = ::secondary,
        )
    }
}
