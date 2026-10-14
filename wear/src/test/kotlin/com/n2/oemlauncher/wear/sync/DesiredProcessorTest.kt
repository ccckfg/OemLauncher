package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.wear.data.ConfigWriter
import com.n2.oemlauncher.wear.data.IconFileStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DesiredProcessorTest {
    private lateinit var processor: DesiredProcessor

    private class FakeWriter : ConfigWriter {
        override suspend fun save(config: LauncherConfig) = Unit
    }

    @Before
    fun setUp() {
        processor = DesiredProcessor(FakeWriter(), IconFileStore(File(System.getProperty("java.io.tmpdir"), "icons-test-${System.nanoTime()}").apply { mkdirs() }))
    }

    @Test
    fun rejectsUnsupportedSchemaAndMalformedPayload() = runTest {
        val state = WatchConfigState(LauncherConfig())
        assertEquals(ApplyResult.REJECTED_SCHEMA, processor.process(99, byteArrayOf(), emptyMap(), state, 1).state.lastResult)
        assertEquals(ApplyResult.REJECTED_INVALID, processor.process(ProtocolVersion.CURRENT, byteArrayOf(1), emptyMap(), state, 1).state.lastResult)
    }

    @Test
    fun ignoresRedeliveryAndRejectsStale() = runTest {
        val state = WatchConfigState(LauncherConfig(revision = 4), lastDesiredRevision = 4)
        val desired = LauncherConfig(revision = 4)
        val payload = PayloadCodec.encodeConfig(desired)
        assertFalse(processor.process(1, payload, emptyMap(), state, 1).applied)
        val stale = processor.process(1, PayloadCodec.encodeConfig(LauncherConfig(revision = 3)), emptyMap(), WatchConfigState(LauncherConfig(revision = 4), lastDesiredRevision = 0), 1)
        assertEquals(ApplyResult.REJECTED_STALE, stale.state.lastResult)
    }

    @Test
    fun appliesAndReportsMissingIcons() = runTest {
        val raw = "icon".encodeToByteArray()
        val iconId = IconId.of(raw)
        val desired = LauncherConfig(revision = 2, iconOverrides = mapOf("com.example/.Main" to iconId))
        val state = WatchConfigState(LauncherConfig())
        val missing = processor.process(1, PayloadCodec.encodeConfig(desired), emptyMap(), state, 2)
        assertEquals(ApplyResult.APPLIED_MISSING_ICONS, missing.state.lastResult)
        assertTrue(missing.applied)
        val applied = processor.process(1, PayloadCodec.encodeConfig(desired.copy(revision = 3)), mapOf(iconId to raw), missing.state, 3)
        assertEquals(ApplyResult.APPLIED, applied.state.lastResult)
    }
}
