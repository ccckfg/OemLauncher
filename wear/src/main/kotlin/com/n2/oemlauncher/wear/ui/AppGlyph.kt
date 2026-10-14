package com.n2.oemlauncher.wear.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import com.n2.oemlauncher.shared.model.CatalogEntry

@Composable
fun AppGlyph(entry: CatalogEntry, state: LauncherUiState, modifier: Modifier, bitmap: Bitmap? = null) {
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(Color(0xFF1A2940), radius)
            image?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt())) }
            if (entry.key in state.config.favorites) drawCircle(Color(0xFF64D8FF), radius - 1.5f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            if (bitmap == null) drawCircle(Color(0xFF4FC3F7), radius * 0.28f, center = Offset(size.width / 2, size.height / 2))
        }
    }
}
