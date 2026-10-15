package com.n2.oemlauncher.wear.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap

object DemoIconFactory {
    private val palette = intArrayOf(0xFF4FC3F7.toInt(), 0xFFFFB74D.toInt(), 0xFF81C784.toInt(), 0xFFE57373.toInt(), 0xFFBA68C8.toInt())

    fun create(label: String): Bitmap {
        val bitmap = createBitmap(128, 128)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = palette[(label.hashCode() and Int.MAX_VALUE) % palette.size]
        canvas.drawRoundRect(RectF(6f, 6f, 122f, 122f), 30f, 30f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        // Demo labels end in their index; showing it keeps 128 tiles distinguishable at a glance.
        val text = label.filter { it.isDigit() }.ifEmpty {
            label.firstOrNull()?.uppercaseChar()?.toString() ?: "•"
        }
        paint.textSize = if (text.length > 2) 34f else 48f
        canvas.drawText(text, 64f, 81f, paint)
        return bitmap
    }
}
