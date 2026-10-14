package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.SyncKeys
import com.n2.oemlauncher.wear.config.WearConstants
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPublisherTest {

    @Test
    fun deduplicatesIdenticalContentButForceAlwaysSends() = runTest {
        val client = RecordingClient()
        val publisher = CatalogPublisher(client)
        val catalog = testCatalog()
        assertEquals(CatalogPublishOutcome.SENT, publisher.publish(catalog, emptyMap(), force = false, now = 1_000))
        assertEquals(CatalogPublishOutcome.SKIPPED_DUPLICATE, publisher.publish(catalog, emptyMap(), force = false, now = 2_000))
        // Only the volatile timestamp moved: still the same content.
        assertEquals(
            CatalogPublishOutcome.SKIPPED_DUPLICATE,
            publisher.publish(catalog.copy(generatedAtMillis = 9), emptyMap(), force = false, now = 20_000),
        )
        assertEquals(CatalogPublishOutcome.SENT, publisher.publish(catalog, emptyMap(), force = true, now = 20_000))
        assertEquals(2, client.catalogPuts().size)
    }

    @Test
    fun failedSendDoesNotRecordTheDigest() = runTest {
        val client = RecordingClient().apply { failPuts = true }
        val publisher = CatalogPublisher(client)
        val catalog = testCatalog()
        assertEquals(CatalogPublishOutcome.FAILED, publisher.publish(catalog, emptyMap(), force = false, now = 1_000))
        client.failPuts = false
        assertEquals(CatalogPublishOutcome.SENT, publisher.publish(catalog, emptyMap(), force = false, now = 2_000))
    }

    @Test
    fun mapsIconIdsToProtocolAssetKeys() = runTest {
        val client = RecordingClient()
        val publisher = CatalogPublisher(client)
        val bytes = "png".encodeToByteArray()
        val iconId = IconId.of(bytes)
        publisher.publish(testCatalog(), mapOf(iconId to bytes, "not-an-id" to bytes), force = true, now = 1_000)
        val assets = client.catalogPuts().single().assets
        assertEquals(setOf(SyncKeys.iconAssetKey(iconId)), assets.keys)
        assertEquals(Asset.createFromBytes(bytes), assets.getValue(SyncKeys.iconAssetKey(iconId)))
    }

    @Test
    fun throttleWindowTracksTheLastSuccessfulSend() = runTest {
        val publisher = CatalogPublisher(RecordingClient())
        assertEquals(0L, publisher.millisUntilWindowEnd(1_000))
        publisher.publish(testCatalog(), emptyMap(), force = false, now = 1_000)
        val windowEnd = 1_000 + WearConstants.CATALOG_THROTTLE_MS
        assertTrue(publisher.isThrottled(windowEnd - 1))
        assertEquals(1L, publisher.millisUntilWindowEnd(windowEnd - 1))
        assertFalse(publisher.isThrottled(windowEnd))
    }
}
