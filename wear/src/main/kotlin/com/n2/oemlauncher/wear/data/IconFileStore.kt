package com.n2.oemlauncher.wear.data

import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import com.n2.oemlauncher.wear.config.WearConstants
import java.io.File
import java.io.IOException

/** Icon bytes on disk, keyed by content id. */
interface IconStore {
    fun put(iconId: String, bytes: ByteArray)

    fun get(iconId: String): ByteArray?

    /** True when the stored file exists and its bytes still hash to [iconId]. */
    fun isValidStored(iconId: String): Boolean
}

/**
 * Icons on disk, one file per content id: default icons published with the catalog and custom
 * icons synced from the phone share this store.
 *
 * Writes go through a temp file and a rename, so a crash can never leave a truncated image
 * behind a valid id; readers either see the whole icon or nothing.
 */
class IconFileStore(private val root: File) : IconStore {
    private val directory = File(root, WearConstants.ICON_CACHE_DIR).apply { mkdirs() }

    override fun put(iconId: String, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val target = File(directory, iconId)
        val temp = File(directory, iconId + TEMP_SUFFIX)
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) {
                temp.delete()
                throw IOException("cannot store icon $iconId")
            }
        }
    }

    override fun get(iconId: String): ByteArray? = File(directory, iconId).takeIf(File::isFile)?.readBytes()

    override fun isValidStored(iconId: String): Boolean {
        val bytes = get(iconId) ?: return false
        return bytes.size <= ProtocolLimits.MAX_ICON_BYTES && IconId.of(bytes) == iconId
    }

    private companion object {
        const val TEMP_SUFFIX = ".tmp"
    }
}
