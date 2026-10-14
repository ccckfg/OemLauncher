package com.n2.oemlauncher.wear.data

import android.util.AtomicFile
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.wear.config.WearConstants
import java.io.File
import java.io.IOException

/**
 * The last published catalog, kept for a fast cold start.
 *
 * Writes are serialized and go through AtomicFile, so the UI refresh and the sync service can
 * write concurrently without one deleting the file the other just committed; a failed write
 * leaves the previous valid snapshot in place.
 */
class CatalogSnapshotStore(private val filesDir: File) {
    private val file = File(filesDir, WearConstants.SNAPSHOT_FILE)
    private val atomicFile = AtomicFile(file)
    private val lock = Any()

    fun read(): WatchCatalog? = synchronized(lock) {
        if (!file.exists()) return null
        val text = try {
            atomicFile.readFully().decodeToString()
        } catch (_: IOException) {
            return null
        }
        PayloadCodec.catalogFromJson(text)
    }

    fun write(catalog: WatchCatalog) {
        val bytes = PayloadCodec.catalogToJson(catalog).encodeToByteArray()
        synchronized(lock) {
            val stream = try {
                atomicFile.startWrite()
            } catch (_: IOException) {
                return
            }
            try {
                stream.write(bytes)
                atomicFile.finishWrite(stream)
            } catch (_: IOException) {
                // Keeps the previous snapshot; a stale catalog beats no catalog on next start.
                atomicFile.failWrite(stream)
            }
        }
    }
}
