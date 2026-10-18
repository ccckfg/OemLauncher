package com.n2.oemlauncher.wear.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.wear.config.WearUiTokens
import com.n2.oemlauncher.shared.model.IconShape

/**
 * App icon at its final shape (the catalog renders circle/squircle/original masks). Icons are
 * decoded larger than most cells, so scaling uses medium filtering to stay smooth.
 */
@Composable
fun AppGlyph(bitmap: Bitmap?, modifier: Modifier, shape: IconShape, favorite: Boolean = false) {
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val density = LocalDensity.current
    val rimPx = with(density) { WearUiTokens.APP_RIM_DP.dp.toPx() }
    val favoritePx = with(density) { WearUiTokens.FAVORITE_RIM_DP.dp.toPx() }
    Spacer(modifier.drawWithCache {
        val glyph = AppGlyphDrawing(shape, size.minDimension, rimPx, favoritePx)
        onDrawBehind {
            drawAppCell(glyph, image, size.width / 2f, size.height / 2f, size.minDimension / 2f, 1f, favorite)
        }
    })
}
