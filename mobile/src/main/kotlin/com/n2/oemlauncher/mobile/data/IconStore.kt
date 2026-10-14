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
    private val staging = File(filesDir, StorageConstants.STAGING_DIRECTORY)
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
            write(requireNotNull(file(expectedId)), bytes, expectedId)
            expectedId
        }

    /** Stage an import icon outside the permanent store until the user confirms. */
    suspend fun putStaged(session: String, bytes: ByteArray, expectedId: String = IconId.of(bytes)): String =
        withContext(Dispatchers.IO) {
            require(bytes.size <= ProtocolLimits.MAX_ICON_BYTES && IconId.isValid(expectedId))
            require(IconId.of(bytes) == expectedId)
            require(session.all { it.isLetterOrDigit() || it == '-' })
            val dir = File(staging, session)
            dir.mkdirs()
            write(File(dir, expectedId + StorageConstants.ICON_SUFFIX), bytes, expectedId)
            expectedId
        }

    /** Atomically move staged icons into the content-addressed store. */
    suspend fun promote(session: String, ids: Collection<String>) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        for (id in ids) {
            val source = File(File(staging, session), id + StorageConstants.ICON_SUFFIX)
            val target = file(id) ?: continue
            if (!source.exists()) continue
            if (target.exists()) source.delete() else check(source.renameTo(target))
        }
    }

    suspend fun discard(session: String) = withContext(Dispatchers.IO) {
        File(staging, session).deleteRecursively()
        Unit
    }

    /** Delete icons nothing references anymore, stale temp files and leftover import sessions. */
    suspend fun collectGarbage(keep: Set<String>) = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { candidate ->
            val id = candidate.name.removeSuffix(StorageConstants.ICON_SUFFIX)
            val kept = candidate.name.endsWith(StorageConstants.ICON_SUFFIX) &&
                IconId.isValid(id) && id in keep
            if (!kept) candidate.delete()
        }
        staging.listFiles()?.forEach { it.deleteRecursively() }
        Unit
    }

    private fun write(target: File, bytes: ByteArray, expectedId: String) {
        if (!target.exists()) {
            val temporary = File(target.parentFile, expectedId + StorageConstants.TEMP_ICON_SUFFIX)
            temporary.writeBytes(bytes)
            check(temporary.renameTo(target))
        }
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
