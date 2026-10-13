package com.n2.oemlauncher.mobile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.n2.oemlauncher.mobile.data.*
import com.n2.oemlauncher.mobile.iconpack.*
import com.n2.oemlauncher.shared.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalCoroutinesApi::class)
class IconImportRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val app = CatalogEntry("com.example.app/com.example.app.Main", "Example")
    private val processor = IconProcessor()
    private class MemoryStore : SnapshotStore {
        override suspend fun load() = CompanionSnapshot()
        override suspend fun save(snapshot: CompanionSnapshot) = Unit
    }
    private fun zip(entries: Map<String, ByteArray>): Uri {
        val file = File(temporary.root, "icons.zip")
        ZipOutputStream(file.outputStream()).use { output ->
            for ((name, bytes) in entries) {
                output.putNextEntry(ZipEntry(name)); output.write(bytes); output.closeEntry()
            }
        }
        return Uri.fromFile(file)
    }
    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        return processor.encode(bitmap)
    }
    private fun importer(repository: CompanionRepository) = IconImportRepository(
        ApplicationProvider.getApplicationContext<Application>().contentResolver, repository, processor,
    )
    private fun catalog() = WatchCatalog(1, "test", 36, "1", WatchDisplay(454, 454, 320, true), listOf(app))

    @Test fun packageNamedMatchesWaitForConfirmationAndApplyAsOneUndoStep() = runTest {
        val repository = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repository.receiveCatalog(catalog())
        repository.receiveState(WatchConfigState(LauncherConfig()))
        val importer = importer(repository)
        val result = importer.import(zip(mapOf("com.example.app.png" to png(), "unmatched.png" to png())))
        assertEquals(listOf(app), result.matches.map { it.app })
        assertEquals(listOf("unmatched"), result.unmatched)
        assertTrue(repository.state.value.config.iconOverrides.isEmpty())
        importer.apply(result)
        assertEquals(result.matches.first().iconId, repository.state.value.config.iconOverrides[app.key])
        assertEquals(1, repository.state.value.draft?.undo?.size)
        repository.undo()
        assertTrue(repository.state.value.config.iconOverrides.isEmpty())
    }
    @Test fun appfilterMatchesActivitySpecificImages() = runTest {
        val repository = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repository.receiveCatalog(catalog())
        val xml = """<resources><item component="ComponentInfo{com.example.app/.Main}" drawable="custom"/></resources>"""
        val result = importer(repository).import(zip(mapOf(
            "appfilter.xml" to xml.toByteArray(), "drawable/custom.png" to png(),
        )))
        assertEquals(app.key, result.matches.single().app.key)
        assertTrue(result.unmatched.isEmpty())
    }
    @Test fun fakeImageExtensionIsRejectedBeforeAnyEdit() = runTest {
        val repository = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repository.receiveCatalog(catalog())
        try {
            importer(repository).import(zip(mapOf("com.example.app.png" to byteArrayOf(1, 2, 3))))
            fail("Non-image accepted")
        } catch (e: IconImportException) {
            assertEquals(ImportFailure.IMAGE, e.reason)
        }
        assertTrue(repository.state.value.config.iconOverrides.isEmpty())
    }
}
