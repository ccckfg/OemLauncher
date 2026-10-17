package com.n2.oemlauncher.wear.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.wear.ui.theme.OemColors

/**
 * App icon at its final shape (the catalog renders circle/squircle/original masks). Icons are
 * decoded larger than most cells, so scaling uses medium filtering to stay smooth.
 */
@Composable
fun AppGlyph(bitmap: Bitmap?, modifier: Modifier, favorite: Boolean = false) {
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val density = LocalDensity.current
    val rimPx = with(density) { WearUiTokens.APP_RIM_DP.dp.toPx() }
    val favoritePx = with(density) { WearUiTokens.FAVORITE_RIM_DP.dp.toPx() }
    Canvas(modifier.clip(CircleShape)) {
        if (image == null) {
            drawCircle(OemColors.Placeholder)
        } else {
            drawImage(
                image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
        drawCircle(
            OemColors.RimLight,
            size.minDimension / 2f - rimPx / 2f,
            style = Stroke(rimPx),
        )
        if (favorite) {
            drawCircle(
                OemColors.Accent,
                size.minDimension / 2f - favoritePx / 2f,
                style = Stroke(favoritePx),
            )
        }
    }
}
