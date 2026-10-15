package com.n2.oemlauncher.wear.domain

import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CatalogSignatureTest {
    private fun catalog(vararg entries: CatalogEntry) = WatchCatalog(
        generatedAtMillis = 1L,
        deviceName = "watch",
        sdkInt = 36,
        appVersionName = "1.0",
        display = WatchDisplay(454, 454, 320, true),
        entries = entries.toList(),
    )

    @Test
    fun ignoresTheVolatileTimestamp() {
        val a = catalog(CatalogEntry("com.example/com.example.Main", "App", iconId = "id1"))
        assertEquals(CatalogSignature.of(a), CatalogSignature.of(a.copy(generatedAtMillis = 999)))
    }

    @Test
    fun changesWhenAppsIconsOrLabelsChange() {
        val base = catalog(CatalogEntry("com.example/com.example.Main", "App", iconId = "id1"))
        val newApp = catalog(
            CatalogEntry("com.example/com.example.Main", "App", iconId = "id1"),
            CatalogEntry("com.example2/com.example2.Main", "App2", iconId = "id2"),
        )
        val newIcon = catalog(CatalogEntry("com.example/com.example.Main", "App", iconId = "id9"))
        val newLabel = catalog(CatalogEntry("com.example/com.example.Main", "Renamed", iconId = "id1"))

        assertNotEquals(CatalogSignature.of(base), CatalogSignature.of(newApp))
        assertNotEquals(CatalogSignature.of(base), CatalogSignature.of(newIcon))
        assertNotEquals(CatalogSignature.of(base), CatalogSignature.of(newLabel))
    }
}
