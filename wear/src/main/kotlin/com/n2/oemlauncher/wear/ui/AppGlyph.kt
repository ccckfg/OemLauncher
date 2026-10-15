package com.n2.oemlauncher.wear.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.n2.oemlauncher.wear.ui.theme.OemColors

/**
 * App icon at its final shape (the catalog renders circle/squircle/original masks). Icons are
 * decoded larger than most cells, so scaling uses medium filtering to stay smooth.
 */
@Composable
fun AppGlyph(bitmap: Bitmap?, modifier: Modifier) {
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    Canvas(modifier) {
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
    }
}
