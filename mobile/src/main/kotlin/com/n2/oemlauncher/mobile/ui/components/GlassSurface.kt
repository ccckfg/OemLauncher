package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.design.GlassSpecs

/**
 * What a pane of glass refracts: the page backdrop, or the pane it sits on. Overlays in their
 * own window cannot see the page, so they provide [emptyBackdrop] and keep only the surface.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

/**
 * The page backdrop: the base color, the warm and cool glows, and a few soft pools of light.
 * Smooth gradients alone give liquid glass nothing to bend, so the pools are what its edges
 * visibly refract. Static: it never moves with the scroll (design section 7). The whole page,
 * backdrop and content, is recorded once more for the sheets and dialogs drawn over it.
 */
@Composable
fun GlassPage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val backdrop = rememberLayerBackdrop()
    val page = rememberLayerBackdrop()
    val overlays = remember { GlassOverlayHost() }
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(page)) {
            PageBackdrop(backdrop)
            CompositionLocalProvider(LocalGlassBackdrop provides backdrop, LocalGlassOverlayHost provides overlays) {
                content()
            }
        }
        GlassOverlayLayer(overlays, page)
    }
}

@Composable
private fun PageBackdrop(backdrop: LayerBackdrop) {
    val palette = GlassTheme.palette
    Box(
        Modifier.fillMaxSize().layerBackdrop(backdrop).drawWithCache {
            fun glow(color: Color, x: Float, y: Float, radius: Float, plateau: Float = 0f): Brush {
                val center = Offset(size.width * x, size.height * y)
                return if (plateau <= 0f) {
                    Brush.radialGradient(listOf(color, Color.Transparent), center, size.height * radius)
                } else {
                    // A flat middle then a short fade: a pool with an edge the lens can bend.
                    Brush.radialGradient(
                        0f to color, plateau to color.copy(alpha = color.alpha * PLATEAU_KEEP),
                        1f to Color.Transparent,
                        center = center, radius = size.height * radius,
                    )
                }
            }
            val warm = glow(palette.warmGlow, GlassTokens.WARM_GLOW_X, GlassTokens.WARM_GLOW_Y, GlassTokens.WARM_GLOW_RADIUS)
            val cool = glow(palette.coolGlow, GlassTokens.COOL_GLOW_X, GlassTokens.COOL_GLOW_Y, GlassTokens.COOL_GLOW_RADIUS)
            val pools = GlassTokens.LIGHT_POOLS.map { pool ->
                val tint = if (pool.cool) palette.coolGlow else palette.accent
                glow(tint.copy(alpha = pool.alpha), pool.x, pool.y, pool.radius, GlassTokens.POOL_PLATEAU)
            }
            onDrawBehind {
                drawRect(palette.background)
                drawRect(warm)
                drawRect(cool)
                pools.forEach { drawRect(it) }
            }
        },
    )
}

/**
 * A pane of liquid glass: it lifts the colors under it, carries a translucent tint and catches
 * light on two opposite edges. With [refract] it also bends what lies behind its rim, with a
 * faint spectral fringe; that costs a shader compiled on the main thread per pane, so it is on
 * for cards and tiles and off for capsules, tracks and list rows, where the band would be a
 * few pixels anyway. [champagne] is the solid accent pane, the one per screen (design section
 * 1). [recessed] sinks it instead, for tracks. Turn [shadow] off under a translucent parent:
 * its alpha layer clips the shadow to a visible box. Pass [exported] to let panes laid on this
 * one refract it in turn.
 */
@Composable
fun Modifier.glassSurface(
    corner: Dp,
    capsule: Boolean = false,
    strong: Boolean = false,
    elevated: Boolean = false,
    champagne: Boolean = false,
    recessed: Boolean = false,
    refract: Boolean = !capsule && !recessed,
    shadow: Boolean = elevated || champagne,
    exported: LayerBackdrop? = null,
): Modifier {
    val palette = GlassTheme.palette
    val backdrop = LocalGlassBackdrop.current
    val shape = remember(corner, capsule) { if (capsule) Capsule() else RoundedRectangle(corner) }
    val tint = if (strong) palette.glassStrong else palette.glass
    val surface = remember(palette, strong, champagne) {
        if (champagne) {
            Brush.verticalGradient(listOf(palette.accentLight, palette.accentDeep))
        } else {
            Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = tint.alpha * GlassTokens.LIQUID_TINT_TOP),
                    tint.copy(alpha = tint.alpha * GlassTokens.LIQUID_TINT_BOTTOM),
                ),
            )
        }
    }
    val rim = if (champagne) {
        Color.White.copy(alpha = GlassTokens.CHAMPAGNE_SPECULAR)
    } else {
        palette.rimLight.copy(alpha = (palette.rimLight.alpha * GlassTokens.SPECULAR_GAIN).coerceAtMost(1f))
    }
    val specular = remember { SpecularRim() }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            if (refract) {
                val edge = if (capsule) GlassTokens.LENS_EDGE_CAPSULE else GlassTokens.LENS_EDGE
                lens(edge.dp.toPx(), (edge * GlassTokens.LENS_STRENGTH).dp.toPx(), chromaticAberration = true)
            }
        },
        highlight = null,
        shadow = if (shadow) {
            { Shadow(radius = GlassTokens.SHADOW_RADIUS.dp, color = Color.Black.copy(alpha = GlassSpecs.Phone.shadowAlpha)) }
        } else {
            null
        },
        // Tracks sit into the glass rather than on it: a shadow along their upper inner edge.
        innerShadow = if (recessed) {
            { InnerShadow(radius = GlassTokens.RECESS_SHADOW.dp, color = Color.Black.copy(alpha = GlassTokens.RECESS_ALPHA)) }
        } else {
            null
        },
        exportedBackdrop = exported,
        onDrawSurface = {
            drawRect(surface, alpha = if (champagne) GlassTokens.CHAMPAGNE_OPACITY else 1f)
            specular.draw(this, shape, rim, GlassTokens.HIGHLIGHT_WIDTH.dp.toPx())
        },
    )
}

/**
 * A glass card with the standard page rhythm inside. It exports itself, so glass controls on
 * it (segments, sliders) refract the card rather than punching through to the page.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = GlassTokens.CARD_CORNER.dp,
    strong: Boolean = false,
    elevated: Boolean = false,
    padding: PaddingValues = PaddingValues(GlassTokens.CARD_PADDING.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val exported = rememberLayerBackdrop()
    Box(
        modifier.fillMaxWidth()
            .glassSurface(corner, strong = strong, elevated = elevated, exported = exported)
            .padding(padding),
    ) {
        CompositionLocalProvider(LocalGlassBackdrop provides exported) { content() }
    }
}

private const val PLATEAU_KEEP = 0.85f
