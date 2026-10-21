package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.shared.design.Palettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** How far a knob is pressed, 0..1, sprung so it swells and settles like a drop of liquid. */
@Stable
class KnobPress internal constructor(private val scope: CoroutineScope) {
    private val progress = Animatable(0f, VISIBILITY)

    val value: Float get() = progress.value

    fun press() {
        scope.launch { progress.animateTo(1f, spring(PRESS_DAMPING, PRESS_STIFFNESS, VISIBILITY)) }
    }

    fun release() {
        scope.launch { progress.animateTo(0f, spring(PRESS_DAMPING, PRESS_STIFFNESS, VISIBILITY)) }
    }

    private companion object {
        const val VISIBILITY = 0.001f
        const val PRESS_DAMPING = 0.6f
        const val PRESS_STIFFNESS = 500f
    }
}

@Composable
fun rememberKnobPress(): KnobPress {
    val scope = rememberCoroutineScope()
    return remember(scope) { KnobPress(scope) }
}

/**
 * The knob of a slider or switch, or the pill of a segmented control: a solid glass bead at
 * rest (cream, or [fill]). Held, it swells by [pressedScale] and clears into a lens, so the
 * track underneath shows through it bent and fringed, the way liquid glass does. [backdrop]
 * should hold the track, so the lens has something to refract.
 */
@Composable
fun Modifier.liquidKnob(
    press: KnobPress,
    backdrop: Backdrop,
    fill: Brush? = null,
    pressedScale: Float = GlassTokens.KNOB_PRESSED_SCALE,
): Modifier {
    val surface = fill ?: SolidColor(Color(Palettes.Mocha.textPrimary))
    val shape = remember { Capsule() }
    val rim = Color.White.copy(alpha = GlassTokens.KNOB_SPECULAR)
    val specular = remember { SpecularRim() }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            val held = press.value
            lens(
                GlassTokens.KNOB_LENS_EDGE.dp.toPx() * held,
                GlassTokens.KNOB_LENS_AMOUNT.dp.toPx() * held,
                chromaticAberration = true,
            )
        },
        highlight = null,
        shadow = {
            Shadow(radius = GlassTokens.KNOB_SHADOW.dp, color = Color.Black.copy(alpha = GlassTokens.KNOB_SHADOW_ALPHA))
        },
        innerShadow = {
            val held = press.value
            InnerShadow(radius = GlassTokens.KNOB_INNER_SHADOW.dp * held, alpha = held)
        },
        layerBlock = {
            val scale = lerp(1f, pressedScale, press.value)
            scaleX = scale
            scaleY = scale
        },
        onDrawSurface = {
            drawRect(surface, alpha = 1f - press.value * GlassTokens.KNOB_CLEARING)
            specular.draw(this, shape, rim, GlassTokens.HIGHLIGHT_WIDTH.dp.toPx())
        },
    )
}
