package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.protocol.DecodeResult
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.SyncKeys
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WearSyncCatalogTest {

    @Test
    fun catalogPublishUsesProtocolAssetKeys() = runTest {
        val bytes = "png".encodeToByteArray()
        val iconId = IconId.of(bytes)
        val client = RecordingClient()
        val repository = testRepository(
            client = client,
            scope = this,
            provider = CatalogProvider { CatalogBundle(testCatalog(), mapOf(iconId to bytes)) },
        )

        repository.publishCatalogNow()

        val put = client.catalogPuts().single()
        assertEquals(setOf(SyncKeys.iconAssetKey(iconId)), put.assets.keys)
        assertEquals(ProtocolVersion.CURRENT, put.schema)
    }

    @Test
    fun nonForcedPublishIsThrottledAndFlushesTheLatestContentAtWindowEnd() = runTest {
        var now = 1_000L
        var current = testCatalog(stamp = 1)
        val client = RecordingClient()
        val repository = testRepository(
            client = client,
            scope = this,
            clock = { now },
            provider = CatalogProvider { CatalogBundle(current, emptyMap()) },
        )

        repository.requestCatalogPublish()
        runCurrent()
        assertEquals(1, client.catalogPuts().size)

        current = testCatalog(stamp = 2).copy(deviceName = "changed")
        now = 2_000L
        repository.requestCatalogPublish()
        runCurrent()
        assertEquals(1, client.catalogPuts().size)

        now = 11_000L
        advanceTimeBy(9_000)
        runCurrent()
        assertEquals(2, client.catalogPuts().size)
        assertEquals("changed", catalogOf(client.catalogPuts().last()).deviceName)
    }

    @Test
    fun nonForcedPublishSkipsContentThatOnlyMovedTheTimestamp() = runTest {
        var now = 1_000L
        var current = testCatalog(stamp = 1)
        val client = RecordingClient()
        val repository = testRepository(
            client = client,
            scope = this,
            clock = { now },
            provider = CatalogProvider { CatalogBundle(current, emptyMap()) },
        )

        repository.requestCatalogPublish()
        runCurrent()
        assertEquals(1, client.catalogPuts().size)

        current = testCatalog(stamp = 999)
        now = 30_000L
        repository.requestCatalogPublish()
        runCurrent()
        assertEquals(1, client.catalogPuts().size)
    }

    @Test
    fun forcedPublishBypassesThrottleAndDedupe() = runTest {
        val client = RecordingClient()
        val repository = testRepository(client = client, scope = this)

        repository.publishCatalogNow()
        repository.publishCatalogNow()

        assertEquals(2, client.catalogPuts().size)
    }

    @Test
    fun failedCatalogPublishKeepsTheChangePending() = runTest {
        val client = RecordingClient().apply { failPuts = true }
        val repository = testRepository(client = client, scope = this)

        repository.publishCatalogNow()
        assertTrue(client.catalogPuts().isEmpty())
        assertEquals(false, repository.status.value.connected)

        client.failPuts = false
        repository.retryPending()

        assertEquals(1, client.catalogPuts().size)
        assertEquals(true, repository.status.value.connected)
    }

    @Test
    fun publishRendersTheAppliedConfig() = runTest {
        var seen: LauncherConfig? = null
        val repository = testRepository(
            scope = this,
            provider = CatalogProvider { config ->
                seen = config
                CatalogBundle(testCatalog(), emptyMap())
            },
        )
        repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 7)),
            emptyMap(),
        )

        repository.publishCatalogNow()

        assertEquals(7L, seen?.revision)
    }

    private fun catalogOf(put: PutCall): WatchCatalog =
        (PayloadCodec.decodeCatalog(ProtocolVersion.CURRENT, put.payload) as DecodeResult.Success).value
}
