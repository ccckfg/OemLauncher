package com.n2.oemlauncher.wear.catalog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.wear.config.WearUiTokens

/** Bitmap work runs only on the catalog's IO dispatcher, never in a drawing callback. */
internal object IconBitmapRenderer {
    fun decode(bytes: ByteArray, size: Int, shape: IconShape): Bitmap? = runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let {
            val resized = if (it.width == size && it.height == size) it else it.scale(size, size)
            mask(resized, shape)
        }
    }.getOrNull()

    fun render(drawable: Drawable, size: Int, shape: IconShape): Bitmap {
        if (drawable !is AdaptiveIconDrawable || shape == IconShape.ORIGINAL) {
            return mask(drawable.toBitmap(size, size, Bitmap.Config.ARGB_8888), shape)
        }
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        canvas.clipPath(maskPath(size, shape))
        // Drawing the adaptive layers bypasses the platform's fixed circular mask.
        val inset = (size * AdaptiveIconDrawable.getExtraInsetFraction()).toInt()
        drawable.background?.mutate()?.apply {
            setBounds(-inset, -inset, size + inset, size + inset)
            draw(canvas)
        }
        drawable.foreground?.mutate()?.apply {
            setBounds(-inset, -inset, size + inset, size + inset)
            draw(canvas)
        }
        return bitmap
    }

    fun hasMonochrome(drawable: Drawable?): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            drawable is AdaptiveIconDrawable && drawable.monochrome != null

    /** The app's monochrome layer in cream on a mocha disc, or null when it has none. */
    fun themed(drawable: Drawable, size: Int, shape: IconShape): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val monochrome = (drawable as? AdaptiveIconDrawable)?.monochrome?.mutate() ?: return null
        return MochaIconRenderer.monochrome(drawable, monochrome, size, shape)
    }

    private fun mask(source: Bitmap, shape: IconShape): Bitmap {
        if (shape == IconShape.ORIGINAL) return source
        val result = createBitmap(source.width, source.height)
        val canvas = Canvas(result)
        canvas.clipPath(maskPath(source.width, shape))
        canvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        return result
    }

    fun maskPath(size: Int, shape: IconShape): Path = Path().apply {
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        if (shape == IconShape.CIRCLE) addOval(rect, Path.Direction.CW)
        else {
            val corner = size * WearUiTokens.ICON_CORNER_FRACTION
            addRoundRect(rect, corner, corner, Path.Direction.CW)
        }
    }
}
