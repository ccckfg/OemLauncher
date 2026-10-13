package com.n2.oemlauncher.mobile.data

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.n2.oemlauncher.mobile.config.StorageConstants
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class IconStore(filesDir: File) {
    private val directory = File(filesDir, StorageConstants.ICON_DIRECTORY)
    private val cache = object : LruCache<String, ImageBitmap>(StorageConstants.BITMAP_CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun file(id: String?): File? = id?.takeIf(IconId::isValid)?.let {
        File(directory, it + StorageConstants.ICON_SUFFIX)
    }

    suspend fun put(bytes: ByteArray, expectedId: String = IconId.of(bytes)): String =
        withContext(Dispatchers.IO) {
            require(bytes.size <= ProtocolLimits.MAX_ICON_BYTES && IconId.isValid(expectedId))
            require(IconId.of(bytes) == expectedId)
            directory.mkdirs()
            val target = requireNotNull(file(expectedId))
            if (!target.exists()) {
                val temporary = File(directory, expectedId + StorageConstants.TEMP_ICON_SUFFIX)
                temporary.writeBytes(bytes)
                check(temporary.renameTo(target))
            }
            expectedId
        }

    suspend fun bytes(id: String): ByteArray? = withContext(Dispatchers.IO) {
        val target = file(id) ?: return@withContext null
        if (!target.exists() || target.length() > ProtocolLimits.MAX_ICON_BYTES) return@withContext null
        target.readBytes().takeIf { IconId.of(it) == id }
    }

    suspend fun bitmap(id: String): ImageBitmap? = withContext(Dispatchers.IO) {
        cache.get(id)?.let { return@withContext it }
        val bytes = bytes(id) ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..ProtocolLimits.CUSTOM_ICON_PX ||
            bounds.outHeight !in 1..ProtocolLimits.CUSTOM_ICON_PX
        ) return@withContext null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.also { cache.put(id, it) }
    }
}
