package com.n2.oemlauncher.mobile.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.MotionTokens
import com.n2.oemlauncher.mobile.config.ThemeTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.design.GlassSpecs
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Sheets and dialogs drawn in the page's own window, not in a window of their own: only there
 * can their glass blur and bend the page under them. One overlay at a time, the latest wins.
 */
@Stable
class GlassOverlayHost {
    internal var current by mutableStateOf<OverlayRequest?>(null)
}

internal class OverlayRequest(val sheet: Boolean) {
    var onDismiss by mutableStateOf({})
    var content by mutableStateOf<@Composable ColumnScope.() -> Unit>({})
}

val LocalGlassOverlayHost = staticCompositionLocalOf { GlassOverlayHost() }

/** Shows [content] in the host for as long as this call stays in the composition. */
@Composable
internal fun ShowOverlay(sheet: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val host = LocalGlassOverlayHost.current
    val request = remember(sheet) { OverlayRequest(sheet) }
    SideEffect {
        request.onDismiss = onDismissRequest
        request.content = content
    }
    DisposableEffect(host, request) {
        host.current = request
        onDispose { if (host.current === request) host.current = null }
    }
}

/**
 * Draws the host's overlay over the page. [page] must record everything under it (backdrop and
 * content) so the overlay's glass has the page to blur.
 */
@Composable
fun BoxScope.GlassOverlayLayer(host: GlassOverlayHost, page: Backdrop) {
    val request = host.current
    // Keep drawing the last overlay while it animates out after its caller has gone.
    val last = remember { arrayOfNulls<OverlayRequest>(1) }
    if (request != null) last[0] = request
    val shown = last[0] ?: return
    val visible = request != null
    BackHandler(enabled = visible) { shown.onDismiss() }
    AnimatedVisibility(visible, Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = GlassTokens.SCRIM_ALPHA))
                .clickable(remember { MutableInteractionSource() }, indication = null) { shown.onDismiss() },
        )
    }
    if (shown.sheet) {
        AnimatedVisibility(
            visible, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(MotionTokens.SHEET_DAMPING, MotionTokens.SHEET_STIFFNESS)) { it },
            exit = slideOutVertically(tween(MotionTokens.SHEET_EXIT_MS)) { it },
        ) { Sheet(shown, page) }
    } else {
        AnimatedVisibility(
            visible, Modifier.align(Alignment.Center),
            enter = fadeIn() + scaleIn(spring(MotionTokens.SHEET_DAMPING, MotionTokens.SHEET_STIFFNESS), MotionTokens.DIALOG_SCALE),
            exit = fadeOut() + scaleOut(targetScale = MotionTokens.DIALOG_SCALE),
        ) {
            val pane = rememberLayerBackdrop()
            Column(
                Modifier.padding(UiDimensions.PAGE_PADDING.dp)
                    .overlayGlass(page, GlassTokens.CARD_CORNER.dp, pane)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .padding(UiDimensions.PAGE_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
            ) {
                CompositionLocalProvider(LocalGlassBackdrop provides pane) { shown.content(this) }
            }
        }
    }
}

/** A floating glass sheet: drag it down past a third, or fling it, and it lets go. */
@Composable
private fun Sheet(request: OverlayRequest, page: Backdrop) {
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var height by remember { mutableIntStateOf(1) }
    val pane = rememberLayerBackdrop()
    val state = rememberDraggableState { delta -> scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) } }
    Column(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(GlassTokens.SHEET_MARGIN.dp)
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            .offset { IntOffset(0, drag.value.roundToInt()) }
            .draggable(
                state, Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (drag.value > height * MotionTokens.SHEET_DISMISS_FRACTION ||
                        velocity > MotionTokens.SHEET_DISMISS_VELOCITY
                    ) {
                        request.onDismiss()
                    } else {
                        drag.animateTo(0f, spring(MotionTokens.SHEET_DAMPING, MotionTokens.SHEET_STIFFNESS))
                    }
                },
            )
            .overlayGlass(page, GlassTokens.SHEET_CORNER.dp, pane)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        SheetHandle()
        CompositionLocalProvider(LocalGlassBackdrop provides pane) { request.content(this) }
    }
}

/**
 * Overlay glass: blurs and lifts the page beneath, bends it at the rim and catches the light.
 * Without blur (before Android 12) the page would sit sharp behind the text, so the tint goes
 * nearly opaque instead.
 */
@Composable
private fun Modifier.overlayGlass(page: Backdrop, corner: Dp, exported: LayerBackdrop): Modifier {
    val palette = GlassTheme.palette
    val shape = remember(corner) { RoundedRectangle(corner) }
    val specular = remember { SpecularRim() }
    val blurs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val tint = if (blurs) palette.glassStrong.copy(alpha = palette.glassStrong.alpha * GlassTokens.OVERLAY_TINT)
    else palette.strongOverBackground
    val rim = palette.rimLight.copy(alpha = (palette.rimLight.alpha * GlassTokens.SPECULAR_GAIN).coerceAtMost(1f))
    return drawBackdrop(
        backdrop = page,
        shape = { shape },
        effects = {
            vibrancy()
            blur(ThemeTokens.OVERLAY_BLUR_DP.dp.toPx())
            lens(GlassTokens.LENS_EDGE.dp.toPx(), (GlassTokens.LENS_EDGE * GlassTokens.LENS_STRENGTH).dp.toPx(), chromaticAberration = true)
        },
        highlight = null,
        shadow = { Shadow(radius = GlassTokens.SHADOW_RADIUS.dp, color = Color.Black.copy(alpha = GlassSpecs.Phone.shadowAlpha)) },
        exportedBackdrop = exported,
        onDrawSurface = {
            drawRect(tint)
            specular.draw(this, shape, rim, GlassTokens.HIGHLIGHT_WIDTH.dp.toPx())
        },
    )
}
