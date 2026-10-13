package com.n2.oemlauncher.shared.protocol

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.model.WatchDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadCodecTest {

    private val schema = ProtocolVersion.CURRENT
    private val config = LauncherConfig(revision = 3, favorites = listOf("com.a/com.a.Main"))

    @Test
    fun configRoundTrips() {
        val decoded = PayloadCodec.decodeConfig(schema, PayloadCodec.encodeConfig(config))
        assertEquals(DecodeResult.Success(config), decoded)
    }

    @Test
    fun stateRoundTrips() {
        val state = WatchConfigState(current = config, lastDesiredRevision = 3, lastResult = ApplyResult.APPLIED)
        assertEquals(state, PayloadCodec.decodeState(schema, PayloadCodec.encodeState(state)).getOrNull())
    }

    @Test
    fun largeCatalogStillFitsOneDataItem() {
        val entries = (0 until ProtocolLimits.MAX_CATALOG_ENTRIES).map {
            CatalogEntry(
                key = "com.vendor.product$it/com.vendor.product$it.ui.MainLauncherActivity",
                label = "Application number $it",
                iconId = IconId.of(byteArrayOf(it.toByte(), (it shr 8).toByte())),
                launchCount = it,
                lastLaunchedMillis = 1_700_000_000_000L + it,
            )
        }
        val catalog = WatchCatalog(1, "Watch", 36, "1.0.0", WatchDisplay(454, 454, 320, true), entries)
        val bytes = PayloadCodec.encodeCatalog(catalog)
        assertTrue("payload ${bytes.size} too large", PayloadCodec.fitsDataItem(bytes))
        assertEquals(catalog, PayloadCodec.decodeCatalog(schema, bytes).getOrNull())
    }

    @Test
    fun unknownFieldsAndEnumValuesAreTolerated() {
        val text = """{"revision":5,"futureField":true,"layout":{"mode":"SPIRAL3D","gridColumns":4}}"""
        val decoded = PayloadCodec.decodeConfig(schema, GzipCodec.compress(text.encodeToByteArray())).getOrNull()
        assertEquals(5L, decoded?.revision)
        assertEquals(LayoutMode.HONEYCOMB, decoded?.layout?.mode)
        assertEquals(4, decoded?.layout?.gridColumns)
    }

    @Test
    fun rejectsUnsupportedSchemaGarbageAndBombs() {
        val bytes = PayloadCodec.encodeConfig(config)
        assertFailure(DecodeFailure.UNSUPPORTED_SCHEMA, PayloadCodec.decodeConfig(ProtocolVersion.CURRENT + 1, bytes))
        assertFailure(DecodeFailure.MALFORMED, PayloadCodec.decodeConfig(schema, byteArrayOf(1, 2, 3)))
        val notJson = GzipCodec.compress("not json".encodeToByteArray())
        assertFailure(DecodeFailure.MALFORMED, PayloadCodec.decodeConfig(schema, notJson))
        val bomb = GzipCodec.compress(ByteArray(ProtocolLimits.MAX_INFLATED_BYTES + 1))
        assertFailure(DecodeFailure.TOO_LARGE, PayloadCodec.decodeConfig(schema, bomb))
        val oversized = ByteArray(ProtocolLimits.MAX_PAYLOAD_BYTES + 1)
        assertFailure(DecodeFailure.TOO_LARGE, PayloadCodec.decodeConfig(schema, oversized))
    }

    @Test
    fun localJsonHelpersNeverThrow() {
        assertEquals(config, PayloadCodec.configFromJson(PayloadCodec.configToJson(config)))
        assertNull(PayloadCodec.configFromJson("{broken"))
        assertNull(PayloadCodec.catalogFromJson("[]"))
    }

    private fun assertFailure(expected: DecodeFailure, actual: DecodeResult<*>) {
        assertTrue("expected failure, got $actual", actual is DecodeResult.Failure)
        assertEquals(expected, (actual as DecodeResult.Failure).reason)
    }
}
