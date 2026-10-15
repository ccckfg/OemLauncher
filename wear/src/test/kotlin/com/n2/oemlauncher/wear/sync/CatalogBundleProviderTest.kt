package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.wear.data.UsageRecord
import com.n2.oemlauncher.wear.data.UsageStats
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private const val KEY_A = "com.example.a/com.example.a.Main"
private const val KEY_B = "com.example.b/com.example.b.Main"

/** Catalog source whose rebuild count proves how often the expensive path runs. */
private class FakeCatalogSource(var current: WatchCatalog?) : CatalogSource {
    var rebuilds = 0
        private set

    override fun snapshot(): WatchCatalog? = current

    override suspend fun rebuild(config: LauncherConfig, usage: UsageStats): WatchCatalog {
        rebuilds++
        val rebuilt = catalogOf(CatalogEntry(KEY_A, "A", iconId = "aaaaaaaaaaaaaaaaaaaaaaaa"))
        current = rebuilt
        return rebuilt
    }
}

private fun catalogOf(vararg entries: CatalogEntry) = WatchCatalog(
    generatedAtMillis = 1L,
    deviceName = "watch",
    sdkInt = 36,
    appVersionName = "1.0",
    display = WatchDisplay(454, 454, 320, true),
    entries = entries.toList(),
)

class CatalogBundleProviderTest {

    @Test
    fun publishesFromTheSnapshotAndDiskIconsWithoutRebuilding() = runTest {
        val iconBytes = "png".encodeToByteArray()
        val iconId = "aaaaaaaaaaaaaaaaaaaaaaaa"
        val icons = FakeIconStore().apply { stored[iconId] = iconBytes }
        val source = FakeCatalogSource(catalogOf(CatalogEntry(KEY_A, "A", iconId = iconId), CatalogEntry(KEY_B, "B")))
        val usage: UsageStats = mapOf(KEY_A to UsageRecord(launchCount = 7, lastLaunchedMillis = 42L))
        val provider = CatalogBundleProvider(source, icons) { usage }

        val first = provider.load(LauncherConfig())
        val second = provider.load(LauncherConfig())

        assertEquals(0, source.rebuilds)
        assertArrayEquals(iconBytes, first.icons[iconId])
        assertEquals(1, first.icons.size)
        assertEquals(7, first.catalog.entries.first { it.key == KEY_A }.launchCount)
        assertEquals(42L, first.catalog.entries.first { it.key == KEY_A }.lastLaunchedMillis)
        assertEquals(0, second.catalog.entries.first { it.key == KEY_B }.launchCount)
    }

    @Test
    fun rebuildsOnlyOnceWhenNoSnapshotExists() = runTest {
        val source = FakeCatalogSource(current = null)
        val provider = CatalogBundleProvider(source, FakeIconStore()) { emptyMap() }

        provider.load(LauncherConfig())
        provider.load(LauncherConfig())

        assertEquals(1, source.rebuilds)
    }

    @Test
    fun stampsThePayloadWithTheCurrentTimeButKeepsTheSnapshotContent() = runTest {
        val snapshot = catalogOf(CatalogEntry(KEY_A, "A"))
        val source = FakeCatalogSource(snapshot)
        val provider = CatalogBundleProvider(source, FakeIconStore()) { emptyMap() }

        val bundle = provider.load(LauncherConfig())

        assertEquals(KEY_A, bundle.catalog.entries.single().key)
        assertFalse(bundle.catalog.generatedAtMillis == snapshot.generatedAtMillis)
    }
}
