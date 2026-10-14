package com.n2.oemlauncher.wear.data

import com.n2.oemlauncher.wear.config.WearConstants
import java.io.File

class IconFileStore(private val root: File) {
    private val directory = File(root, WearConstants.ICON_CACHE_DIR).apply { mkdirs() }

    fun put(iconId: String, bytes: ByteArray) {
        if (bytes.isNotEmpty()) File(directory, iconId).writeBytes(bytes)
    }

    fun get(iconId: String): ByteArray? = File(directory, iconId).takeIf(File::isFile)?.readBytes()

    fun contains(iconId: String): Boolean = File(directory, iconId).isFile
}
