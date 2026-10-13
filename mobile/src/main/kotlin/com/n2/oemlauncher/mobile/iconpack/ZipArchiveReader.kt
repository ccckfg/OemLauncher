package com.n2.oemlauncher.mobile.iconpack

import com.n2.oemlauncher.mobile.config.ImportLimits
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

enum class ImportFailure { PATH, COUNT, FILE_SIZE, TOTAL_SIZE, FILE_TYPE, XML, IMAGE }
class IconImportException(val reason: ImportFailure) : Exception(reason.name)
data class RawIconPack(val images: Map<String, ByteArray>, val filters: Map<String, String>)

class ZipArchiveReader {
    fun read(input: InputStream): RawIconPack {
        val images = LinkedHashMap<String, ByteArray>()
        var xml: ByteArray? = null
        var count = 0
        var total = 0L
        val buffer = ByteArray(ImportLimits.BUFFER_BYTES)
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                count++
                if (count > ImportLimits.MAX_ENTRIES) fail(ImportFailure.COUNT)
                val name = entry.name
                if (name.startsWith('/') || name.contains('\\') || name.contains(':') ||
                    name.split('/').any { it == ".." || it == "." } || name.contains('\u0000')
                ) fail(ImportFailure.PATH)
                if (entry.isDirectory) {
                    // nextEntry drains unread data. Reject disguised payloads before it can inflate them.
                    if (zip.read() != -1) fail(ImportFailure.FILE_TYPE)
                    continue
                }
                val basename = name.substringAfterLast('/')
                val isXml = basename == "appfilter.xml"
                val extension = basename.substringAfterLast('.', "").lowercase()
                if (!isXml && extension !in ImportLimits.IMAGE_EXTENSIONS) fail(ImportFailure.FILE_TYPE)
                if (entry.size > ImportLimits.MAX_FILE_BYTES) fail(ImportFailure.FILE_SIZE)
                val output = ByteArrayOutputStream()
                var size = 0
                while (true) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    size += read
                    total += read
                    if (size > ImportLimits.MAX_FILE_BYTES) fail(ImportFailure.FILE_SIZE)
                    if (total > ImportLimits.MAX_TOTAL_BYTES) fail(ImportFailure.TOTAL_SIZE)
                    output.write(buffer, 0, read)
                }
                if (isXml) {
                    if (xml != null) fail(ImportFailure.XML)
                    xml = output.toByteArray()
                } else {
                    val stem = basename.substringBeforeLast('.')
                    if (images.containsKey(stem)) fail(ImportFailure.FILE_TYPE)
                    images[stem] = output.toByteArray()
                }
            }
        }
        if (images.isEmpty()) fail(ImportFailure.FILE_TYPE)
        return RawIconPack(images, xml?.let(AppFilterParser::parse) ?: emptyMap())
    }

    private fun fail(reason: ImportFailure): Nothing = throw IconImportException(reason)
}
