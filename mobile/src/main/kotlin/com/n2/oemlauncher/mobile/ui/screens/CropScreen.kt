package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.editor.ImportState
import com.n2.oemlauncher.mobile.iconpack.CropTransform
import com.n2.oemlauncher.mobile.ui.components.ChampagneButton
import com.n2.oemlauncher.mobile.ui.components.GlassButton
import com.n2.oemlauncher.mobile.ui.components.GlassSegmented
import com.n2.oemlauncher.mobile.ui.components.GlassSlider
import com.n2.oemlauncher.mobile.ui.components.Processing

@Composable
fun CropScreen(key: String, vm: CompanionViewModel, state: ImportState, done: () -> Unit) {
    val photo = state.photo
    var transform by remember(photo) { mutableStateOf(CropTransform()) }
    var circle by remember { mutableStateOf(true) }
    var edge by remember { mutableIntStateOf(1) }
    val image = remember(photo) { photo?.asImageBitmap() }
    val mask = remember { Path().apply { fillType = PathFillType.EvenOdd } }
    Column(
        Modifier.padding(UiDimensions.PAGE_PADDING.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        Text(
            stringResource(R.string.crop_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.busy) Processing()
        if (photo != null && image != null) {
            Box(
                Modifier.widthIn(max = UiDimensions.PREVIEW_PANE.dp).fillMaxWidth().aspectRatio(1f)
                    .clipToBounds().onSizeChanged { edge = it.width }
                    .pointerInput(photo) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            transform = CropTransform(
                                transform.zoom * zoom, transform.x + pan.x / edge, transform.y + pan.y / edge,
                            ).constrained(photo.width, photo.height)
                        }
                    },
            ) {
                Image(
                    image, stringResource(R.string.crop), contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer {
                        scaleX = transform.zoom; scaleY = transform.zoom
                        translationX = transform.x * edge; translationY = transform.y * edge
                    },
                )
                if (circle) Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                    mask.reset()
                    mask.addRect(Rect(0f, 0f, size.width, size.height))
                    mask.addOval(Rect(0f, 0f, size.width, size.height))
                    drawPath(mask, Color.Black.copy(alpha = UiDimensions.CROP_SCRIM_ALPHA))
                }
            }
            GlassSegmented(
                options = listOf(true, false),
                selected = circle,
                label = { if (it) stringResource(R.string.circle) else stringResource(R.string.square) },
                onSelect = { circle = it },
                modifier = Modifier.widthIn(max = UiDimensions.PREVIEW_PANE.dp),
            )
            GlassSlider(
                value = transform.zoom,
                valueRange = 1f..ImportLimits.MAX_ZOOM,
                onValueChange = { transform = transform.copy(zoom = it).constrained(photo.width, photo.height) },
                onValueChangeFinished = {},
                description = stringResource(R.string.zoom),
                modifier = Modifier.widthIn(max = UiDimensions.PREVIEW_PANE.dp),
            )
            ChampagneButton(
                stringResource(R.string.save_icon),
                { vm.crop(key, transform, circle, done) },
                Modifier.fillMaxWidth(),
                enabled = !state.busy,
            )
        } else if (!state.busy) {
            Text(
                stringResource(R.string.notice_icon_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GlassButton(
            stringResource(R.string.cancel),
            { vm.cancelImport(); done() },
            Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
    }
}
