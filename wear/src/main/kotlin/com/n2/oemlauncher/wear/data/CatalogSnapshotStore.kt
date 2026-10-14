package com.n2.oemlauncher.wear.data

import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.wear.config.WearConstants
import java.io.File

class CatalogSnapshotStore(private val filesDir: File) {
    private val file get() = File(filesDir, WearConstants.SNAPSHOT_FILE)

    fun read(): WatchCatalog? = file.takeIf(File::isFile)?.readText()?.let(PayloadCodec::catalogFromJson)

    fun write(catalog: WatchCatalog) {
        val temp = File(filesDir, "${WearConstants.SNAPSHOT_FILE}.tmp")
        temp.writeText(PayloadCodec.catalogToJson(catalog))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }
}
