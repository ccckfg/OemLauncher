package com.n2.oemlauncher.mobile

import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.mobile.iconpack.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ZipArchiveReaderTest {
    private fun archive(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
                }
            }
            output.toByteArray()
        }

    private fun refused(bytes: ByteArray, failure: ImportFailure) {
        try {
            ZipArchiveReader().read(bytes.inputStream())
            fail("Expected $failure")
        } catch (e: IconImportException) {
            assertEquals(failure, e.reason)
        }
    }

    @Test fun rejectsPathTraversalAndAbsolutePaths() {
        listOf("../escape.png", "a/../../escape.png", "/escape.png", "C:/escape.png", "a\\escape.png").forEach {
            refused(archive(it to byteArrayOf(1)), ImportFailure.PATH)
        }
    }
    @Test fun rejectsNonImages() {
        refused(archive("script.sh" to byteArrayOf(1)), ImportFailure.FILE_TYPE)
        refused(archive("other.xml" to byteArrayOf(1)), ImportFailure.FILE_TYPE)
    }
    @Test fun directoriesCannotBypassInflationLimits() {
        refused(archive("images/" to ByteArray(ImportLimits.MAX_FILE_BYTES + 1)), ImportFailure.FILE_TYPE)
    }
    @Test fun rejectsNonZipDocuments() {
        refused("not a zip".toByteArray(), ImportFailure.FILE_TYPE)
    }
    @Test fun limitsActualInflatedFileSize() {
        refused(archive("big.png" to ByteArray(ImportLimits.MAX_FILE_BYTES + 1)), ImportFailure.FILE_SIZE)
    }
    @Test fun limitsEntries() {
        val entries = Array(ImportLimits.MAX_ENTRIES + 1) { "icon$it.png" to byteArrayOf(1) }
        refused(archive(*entries), ImportFailure.COUNT)
    }
    @Test fun limitsTotalInflatedSize() {
        val data = ByteArray(ImportLimits.MAX_FILE_BYTES)
        val entries = Array(ImportLimits.MAX_TOTAL_BYTES / data.size + 1) { "icon$it.png" to data }
        refused(archive(*entries), ImportFailure.TOTAL_SIZE)
    }
    @Test fun parsesCommonComponentMappingsAndCanonicalizesNames() {
        val xml = """<resources><item component="ComponentInfo{com.example.app/.Main}" drawable="clock"/></resources>"""
        val pack = ZipArchiveReader().read(
            archive("res/appfilter.xml" to xml.toByteArray(), "res/drawable/clock.png" to byteArrayOf(1)).inputStream(),
        )
        assertEquals("clock", pack.filters["com.example.app/com.example.app.Main"])
        assertEquals(setOf("clock"), pack.images.keys)
    }
    @Test fun rejectsXmlEntitiesAndMalformedXml() {
        assertThrows(IconImportException::class.java) {
            AppFilterParser.parse("""<!DOCTYPE x [<!ENTITY a SYSTEM "file:///secret">]><resources/>""".toByteArray())
        }
        assertThrows(IconImportException::class.java) { AppFilterParser.parse("<resources><".toByteArray()) }
    }
    @Test fun acceptsPackageNamesAndRejectsAmbiguousDuplicateStems() {
        val pack = ZipArchiveReader().read(archive("com.example.app.png" to byteArrayOf(1)).inputStream())
        assertTrue(pack.images.containsKey("com.example.app"))
        refused(archive("a/clock.png" to byteArrayOf(1), "b/clock.jpg" to byteArrayOf(1)), ImportFailure.FILE_TYPE)
    }
}
