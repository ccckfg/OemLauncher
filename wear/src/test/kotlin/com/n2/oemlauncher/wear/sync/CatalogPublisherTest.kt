package com.n2.oemlauncher.wear.sync

import android.graphics.Bitmap
import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingClient : DataLayerClient {
    var calls = 0
    override suspend fun put(path: String, schema: Int, payload: ByteArray, assets: Map<String, Asset>, urgent: Boolean): Boolean { calls++; return true }
    override suspend fun send(path: String): Boolean = true
}

class CatalogPublisherTest {
    @Test
    fun deduplicatesAndThrottlesButForcePublishes() = runTest {
        val client = RecordingClient()
        val publisher = CatalogPublisher(client)
        val catalog = WatchCatalog(1, "watch", 36, "1", WatchDisplay(1, 1, 1, true), emptyList())
        assertTrue(publisher.publish(catalog, emptyMap(), now = 1_000))
        assertFalse(publisher.publish(catalog, emptyMap(), now = 2_000))
        assertFalse(publisher.publish(catalog.copy(generatedAtMillis = 2), emptyMap(), now = 3_000))
        assertTrue(publisher.publish(catalog, emptyMap(), force = true, now = 3_000))
        assertEquals(2, client.calls)
    }
}
