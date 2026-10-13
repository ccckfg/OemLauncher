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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.editor.ImportState
import com.n2.oemlauncher.mobile.iconpack.CropTransform
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
        Text(stringResource(R.string.crop_hint))
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
                    mask.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    mask.addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    drawPath(mask, Color.Black.copy(alpha = UiDimensions.CROP_SCRIM_ALPHA))
                }
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp)) {
                FilterChip(circle, { circle = true }, label = { Text(stringResource(R.string.circle)) })
                FilterChip(!circle, { circle = false }, label = { Text(stringResource(R.string.square)) })
            }
            Text(stringResource(R.string.zoom))
            val zoomDescription = stringResource(R.string.zoom)
            Slider(
                transform.zoom,
                { transform = transform.copy(zoom = it).constrained(photo.width, photo.height) },
                valueRange = 1f..ImportLimits.MAX_ZOOM,
                modifier = Modifier.semantics { contentDescription = zoomDescription },
            )
            Button({ vm.crop(key, transform, circle, done) }, enabled = !state.busy) { Text(stringResource(R.string.save_icon)) }
        } else if (!state.busy) Text(stringResource(R.string.notice_icon_failed))
        TextButton({ vm.cancelImport(); done() }, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
    }
}
