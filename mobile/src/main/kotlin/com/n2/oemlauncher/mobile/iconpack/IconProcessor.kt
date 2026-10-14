package com.n2.oemlauncher.mobile.iconpack

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.exifinterface.media.ExifInterface
import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.mobile.config.StorageConstants
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

data class CropTransform(val zoom: Float = 1f, val x: Float = 0f, val y: Float = 0f) {
    fun constrained(width: Int, height: Int): CropTransform {
        val zoom = zoom.coerceIn(1f, ImportLimits.MAX_ZOOM)
        val side = minOf(width, height).toFloat()
        val limitX = (width / side * zoom - 1f) / 2f
        val limitY = (height / side * zoom - 1f) / 2f
        return CropTransform(zoom, x.coerceIn(-limitX, limitX), y.coerceIn(-limitY, limitY))
    }
}

class IconProcessor {
    fun decode(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > ImportLimits.MAX_SOURCE_PIXELS
        ) throw IconImportException(ImportFailure.IMAGE)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > ImportLimits.DECODE_EDGE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IconImportException(ImportFailure.IMAGE)
        val exif = ByteArrayInputStream(bytes).use { ExifInterface(it) }
        val matrix = Matrix().apply {
            if (exif.isFlipped) postScale(-1f, 1f)
            postRotate(exif.rotationDegrees.toFloat())
        }
        if (matrix.isIdentity) return original
        return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true).also {
            if (it !== original) original.recycle()
        }
    }

    fun normalize(bytes: ByteArray): ByteArray {
        val bitmap = decode(bytes)
        return try { crop(bitmap, CropTransform(), circle = false) } finally { bitmap.recycle() }
    }

    fun crop(source: Bitmap, transform: CropTransform, circle: Boolean): ByteArray {
        val side = ProtocolLimits.CUSTOM_ICON_PX
        val output = createBitmap(side, side)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val safe = transform.constrained(source.width, source.height)
        val factor = side.toFloat() / minOf(source.width, source.height) * safe.zoom
        canvas.withTranslation(
            (side - source.width * factor) / 2f + safe.x * side,
            (side - source.height * factor) / 2f + safe.y * side,
        ) {
            scale(factor, factor)
            drawBitmap(source, 0f, 0f, paint)
        }
        if (circle) {
            val mask = createBitmap(side, side)
            Canvas(mask).drawCircle(side / 2f, side / 2f, side / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            canvas.drawBitmap(mask, 0f, 0f, paint)
            mask.recycle()
        }
        return try { encode(output) } finally { output.recycle() }
    }

    fun encode(bitmap: Bitmap): ByteArray {
        fun png(): ByteArray = ByteArrayOutputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, StorageConstants.PNG_QUALITY, it))
            it.toByteArray()
        }
        var bytes = png()
        for (mask in StorageConstants.PNG_QUANTIZATION_MASKS) {
            if (bytes.size <= ProtocolLimits.MAX_ICON_BYTES) break
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (i in pixels.indices) {
                val p = pixels[i]
                pixels[i] = Color.argb(
                    if (Color.alpha(p) == Color.alpha(Color.WHITE)) Color.alpha(p) else Color.alpha(p) and mask,
                    Color.red(p) and mask,
                    Color.green(p) and mask,
                    Color.blue(p) and mask,
                )
            }
            bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            bytes = png()
        }
        require(bytes.size <= ProtocolLimits.MAX_ICON_BYTES)
        return bytes
    }
}
