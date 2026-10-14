package com.n2.oemlauncher.wear.catalog

import android.graphics.Bitmap
import java.util.LinkedHashMap

data class IconCacheKey(val iconId: String, val sizePx: Int, val shape: String)

class IconMemoryCache(private val maxBytes: Long) {
    private val values = object : LinkedHashMap<IconCacheKey, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IconCacheKey, Bitmap>?): Boolean =
            bytes > maxBytes
    }
    private var bytes = 0L

    @Synchronized fun get(key: IconCacheKey): Bitmap? = values[key]

    @Synchronized fun put(key: IconCacheKey, bitmap: Bitmap) {
        values.remove(key)?.let { bytes -= it.allocationByteCount }
        values[key] = bitmap
        bytes += bitmap.allocationByteCount
        trim()
    }

    @Synchronized fun clear() {
        values.values.forEach(Bitmap::recycle)
        values.clear()
        bytes = 0
    }

    private fun trim() {
        val iterator = values.entries.iterator()
        while (bytes > maxBytes && iterator.hasNext()) {
            bytes -= iterator.next().value.allocationByteCount
            iterator.remove()
        }
    }
}
