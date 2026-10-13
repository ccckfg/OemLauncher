package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.DemoConstants
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.data.CompanionState
import com.n2.oemlauncher.mobile.data.IconStore
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.ordering.AppOrdering

@Composable
fun WatchPreview(
    state: CompanionState, store: IconStore, thumbnail: Boolean = false,
    maxEdgeDp: Int = UiDimensions.PREVIEW,
) {
    val catalog = state.catalog ?: return
    // CatalogSanitizer does not validate display facts; malformed peer data must not crash UI.
    val display = remember(catalog.display) {
        catalog.display.takeIf { it.widthPx > 0 && it.heightPx > 0 && it.densityDpi > 0 }
            ?: WatchDisplay(DemoConstants.WIDTH_PX, DemoConstants.WIDTH_PX, DemoConstants.DENSITY_DPI, true)
    }
    val config = state.config
    val apps = remember(catalog.entries, config) { AppOrdering.arrange(catalog.entries, config).all }
    val ids = remember(apps, config.iconOverrides) { apps.map { config.iconOverrides[it.key] ?: it.iconId } }
    val images by produceState<List<Bitmap?>>(emptyList(), ids, state.iconGeneration) {
        value = ids.map { id -> id?.let { store.bitmap(it)?.asAndroidBitmap() } }
    }
    val renderer = remember(apps, display, config.layout, images) {
        PreviewRenderer(apps, display, config.layout, images)
    }
    var panX by remember(config.layout.mode, config.layout.iconSize) { mutableFloatStateOf(0f) }
    var panY by remember(config.layout.mode, config.layout.iconSize) { mutableFloatStateOf(0f) }
    val description = stringResource(R.string.preview)
    val shape = if (display.isRound) CircleShape else RoundedCornerShape(0.dp)
    Column(
        Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
    ) {
        Box(
            Modifier.widthIn(max = (if (thumbnail) UiDimensions.THUMBNAIL else maxEdgeDp).dp)
                .fillMaxWidth().aspectRatio(display.widthPx.toFloat() / display.heightPx)
                .clip(shape).background(MaterialTheme.colorScheme.outlineVariant)
                .padding(UiDimensions.PREVIEW_BORDER.dp),
        ) {
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(display.widthPx.toFloat() / display.heightPx)
                    .clip(shape).semantics { contentDescription = description }
                    .pointerInput(renderer) {
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
                                    panX = renderer.scratch[0]; panY = renderer.scratch[1]
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                val scale = display.widthPx.toFloat() / size.width
                                rawX += amount.x * scale
                                rawY += amount.y * scale
                                if (config.layout.mode == LayoutMode.HONEYCOMB) {
                                    renderer.bounds.rubberBand(rawX, rawY, renderer.scratch)
                                    panX = renderer.scratch[0]; panY = renderer.scratch[1]
                                } else {
                                    panY = rawY.coerceIn(-renderer.scrollLimit, 0f)
                                }
                            },
                        )
                    },
            ) {
                drawIntoCanvas { renderer.draw(it.nativeCanvas, size.width, size.height, panX, panY) }
            }
        }
        if (!thumbnail) {
            Text(
                stringResource(R.string.preview_dimensions, display.widthPx, display.heightPx, display.densityDpi),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { panX = 0f; panY = 0f }) { Text(stringResource(R.string.reset_pan)) }
        }
    }
}
