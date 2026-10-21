package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.DemoConstants
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.PreviewTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.design.GlassSpecs
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.ordering.AppOrdering

/**
 * The watch preview. Pass [diameterDp] for the small bezelled previews inside a card, or leave
 * it null for the full-size one. The gear glyph is rasterised here once and handed to the
 * renderer, which never touches resources.
 */
@Composable
fun WatchPreview(
    state: CompanionState,
    vm: CompanionViewModel,
    modifier: Modifier = Modifier,
    diameterDp: Int? = null,
    maxEdgeDp: Int = GlassTokens.PREVIEW_MAX,
    onOpen: (() -> Unit)? = null,
) {
    val catalog = state.catalog ?: return
    // CatalogSanitizer does not validate display facts; malformed peer data must not crash UI.
    val display = remember(catalog.display) {
        catalog.display.takeIf { it.widthPx > 0 && it.heightPx > 0 && it.densityDpi > 0 }
            ?: WatchDisplay(DemoConstants.WIDTH_PX, DemoConstants.WIDTH_PX, DemoConstants.DENSITY_DPI, true)
    }
    val config = state.config
    val arranged = remember(catalog.entries, config) { AppOrdering.arrange(catalog.entries, config) }
    val apps = arranged.all
    val ids = remember(apps, config.iconOverrides) { apps.map { config.iconOverrides[it.key] ?: it.iconId } }
    val iconImages by vm.iconImages.collectAsStateWithLifecycle()
    LaunchedEffect(ids, state.iconGeneration) { vm.prefetchIcons(ids) }
    val mocha = rememberMochaIcons(apps, config)
    val images = ids.mapIndexed { i, id -> mocha[apps[i].key] ?: id?.let { iconImages[it] }?.asAndroidBitmap() }
    val palette = GlassTheme.palette
    val gear = rememberGearGlyph(palette.onAccent)
    val settingsLabel = stringResource(R.string.settings_label)
    val favorites = arranged.favorites.size
    val renderer = remember(apps, display, config.layout, images, gear, settingsLabel, favorites) {
        PreviewRenderer(apps, display, config.layout, images, gear, settingsLabel, favorites)
    }
    val panKeys: Array<Any> = arrayOf(config.layout.mode, config.layout.iconSize, config.layout.gridColumns)
    var panX by remember(*panKeys) { mutableFloatStateOf(0f) }
    var panY by remember(*panKeys) { mutableFloatStateOf(0f) }
    // A rebuilt renderer can shrink the content; clamp so the preview never shows blank space.
    LaunchedEffect(renderer.scrollLimit, config.layout.mode) {
        if (config.layout.mode != LayoutMode.HONEYCOMB) {
            panY = PreviewRenderer.clampedVerticalPan(panY, renderer.scrollLimit)
        }
    }
    val description = stringResource(R.string.preview)
    val openLabel = stringResource(R.string.preview_tap)
    val shape = if (display.isRound) CircleShape else RoundedCornerShape(0.dp)
    val bezel = diameterDp != null
    val shadow = Color.Black.copy(alpha = GlassSpecs.Phone.shadowAlpha)
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
    ) {
        Box(
            Modifier
                .then(
                    if (bezel) {
                        Modifier.size(diameterDp.dp)
                            .drawBehind {
                                val ring = GlassTokens.BEZEL_RING.dp.toPx()
                                drawCircle(
                                    color = palette.rimLight,
                                    radius = size.minDimension / 2f - ring / 2f,
                                    style = Stroke(ring),
                                )
                            }
                            .padding(GlassTokens.BEZEL_RING.dp)
                    } else {
                        Modifier.widthIn(max = maxEdgeDp.dp).fillMaxWidth()
                            .aspectRatio(display.widthPx.toFloat() / display.heightPx)
                    },
                )
                .shadow(if (bezel) 16.dp else 8.dp, shape, ambientColor = shadow, spotColor = shadow)
                .clip(shape)
                .background(palette.background)
                .semantics { contentDescription = description }
                .then(
                    if (onOpen != null) {
                        Modifier.clickable(onClickLabel = openLabel, onClick = onOpen)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(renderer) {
                    var rawX = panX
                    var rawY = panY
                    detectDragGestures(
                        onDragStart = { rawX = panX; rawY = panY },
                        onDragEnd = {
                            if (config.layout.mode == LayoutMode.HONEYCOMB) {
                                renderer.bounds.clamp(panX, panY, renderer.scratch)
                                if (config.layout.snapToCenter) {
                                    renderer.geometry.snapTarget(renderer.scratch[0], renderer.scratch[1], renderer.scratch)
                                }
                                panX = renderer.scratch[0]
                                panY = renderer.scratch[1]
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            val scale = display.widthPx.toFloat() / size.width
                            rawX += amount.x * scale
                            rawY += amount.y * scale
                            if (config.layout.mode == LayoutMode.HONEYCOMB) {
                                renderer.bounds.rubberBand(rawX, rawY, renderer.scratch)
                                panX = renderer.scratch[0]
                                panY = renderer.scratch[1]
                            } else {
                                panY = PreviewRenderer.clampedVerticalPan(rawY, renderer.scrollLimit)
                            }
                        },
                    )
                },
            ) {
                drawIntoCanvas { renderer.draw(it.nativeCanvas, size.width, size.height, panX, panY) }
            }
        }
        if (!bezel) {
            Text(
                stringResource(R.string.preview_dimensions, display.widthPx, display.heightPx, display.densityDpi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { panX = 0f; panY = 0f },
                colors = glassTextButtonColors(),
            ) { Text(stringResource(R.string.reset_pan)) }
        }
    }
}

/** The gear glyph the watch draws, tinted to the color it sits on and rasterised once. */
@Composable
private fun rememberGearGlyph(color: Color): Bitmap? {
    val context = LocalContext.current
    val argb = color.toArgb()
    return remember(context, argb) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_preview_gear) ?: return@remember null
        val side = PreviewTokens.GEAR_BITMAP_PX
        val bitmap = createBitmap(side, side)
        drawable.setTint(argb)
        drawable.setBounds(0, 0, side, side)
        drawable.draw(AndroidCanvas(bitmap))
        bitmap
    }
}
