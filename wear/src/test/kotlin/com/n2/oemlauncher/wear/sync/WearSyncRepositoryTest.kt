package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.protocol.DecodeResult
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSyncRepositoryTest {

    @Test
    fun restoreIgnoresDesiredOlderThanPersistedState() = runTest {
        val store = FakeSyncStateStore(testPersisted(revision = 10, lastDesired = 10))
        val client = RecordingClient()
        val repository = testRepository(store = store, client = client, scope = this)

        val outcome = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 5)),
            emptyMap(),
        )

        assertFalse(outcome.applied)
        assertTrue(client.puts.isEmpty())
        assertTrue(store.saves.isEmpty())
    }

    @Test
    fun restoreAdoptsConfigNewerThanTheStoredState() = runTest {
        // The UI saved revision 12 on its own after the state was persisted at revision 10.
        val store = FakeSyncStateStore(
            testPersisted(revision = 10, lastDesired = 10).copy(config = LauncherConfig(revision = 12)),
        )
        val client = RecordingClient()
        val repository = testRepository(store = store, client = client, scope = this)

        val outcome = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 11)),
            emptyMap(),
        )

        assertFalse(outcome.applied)
        assertTrue(client.puts.isEmpty())
    }

    @Test
    fun redeliveryPublishesNothing() = runTest {
        val client = RecordingClient()
        val repository = testRepository(client = client, scope = this)
        repository.handleDesired(ProtocolVersion.CURRENT, PayloadCodec.encodeConfig(LauncherConfig(revision = 4)), emptyMap())
        val putsAfterApply = client.puts.size

        val outcome = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 4)),
            emptyMap(),
        )

        assertFalse(outcome.applied)
        assertEquals(putsAfterApply, client.puts.size)
    }

    @Test
    fun appliesDesiredWithIconsAndPublishesReceipt() = runTest {
        val iconBytes = "icon".encodeToByteArray()
        val iconId = IconId.of(iconBytes)
        val asset = Asset.createFromBytes(iconBytes)
        val client = RecordingClient().apply { assetBytes[asset] = iconBytes }
        val icons = FakeIconStore()
        val store = FakeSyncStateStore()
        val repository = testRepository(store = store, client = client, icons = icons, scope = this)
        val desired = LauncherConfig(revision = 2, iconOverrides = mapOf(TEST_APP_KEY to iconId))

        val outcome = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(desired),
            mapOf(iconId to asset),
        )

        assertTrue(outcome.applied)
        assertEquals(ApplyResult.APPLIED, outcome.state.lastResult)
        assertArrayEquals(iconBytes, icons.stored[iconId])
        assertEquals(listOf(true, false), store.saves.map { it.second })
        val receipt = (PayloadCodec.decodeState(ProtocolVersion.CURRENT, client.statePuts().single().payload) as DecodeResult.Success).value
        assertEquals(ApplyResult.APPLIED, receipt.lastResult)
        assertEquals(2L, receipt.current.revision)
    }

    @Test
    fun reportsMissingIconsWithoutBlockingTheConfig() = runTest {
        val iconId = IconId.of("absent".encodeToByteArray())
        val desired = LauncherConfig(revision = 2, iconOverrides = mapOf(TEST_APP_KEY to iconId))
        val repository = testRepository(scope = this)

        val outcome = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(desired),
            mapOf(iconId to Asset.createFromBytes("absent".encodeToByteArray())),
        )

        assertTrue(outcome.applied)
        assertEquals(ApplyResult.APPLIED_MISSING_ICONS, outcome.state.lastResult)
        assertEquals(listOf(iconId), outcome.state.missingIconIds)
    }

    @Test
    fun reusesIconAlreadyOnDiskWithoutReadingTheAsset() = runTest {
        val bytes = "already-there".encodeToByteArray()
        val iconId = IconId.of(bytes)
        val icons = FakeIconStore().apply { stored[iconId] = bytes }
        val client = RecordingClient()
        val repository = testRepository(client = client, icons = icons, scope = this)
        val desired = LauncherConfig(revision = 3, iconOverrides = mapOf(TEST_APP_KEY to iconId))

        val outcome = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(desired),
            mapOf(iconId to Asset.createFromBytes(bytes)),
        )

        assertEquals(ApplyResult.APPLIED, outcome.state.lastResult)
        assertTrue(outcome.state.missingIconIds.isEmpty())
        assertTrue(client.assetReads.isEmpty())
    }

    @Test
    fun failedIconWriteLeavesConfigAndReceiptUntouched() = runTest {
        val bytes = "bad-disk".encodeToByteArray()
        val iconId = IconId.of(bytes)
        val icons = FakeIconStore().apply { failOnPut = iconId }
        val store = FakeSyncStateStore()
        val client = RecordingClient().apply { assetBytes[Asset.createFromBytes(bytes)] = bytes }
        val repository = testRepository(store = store, client = client, icons = icons, scope = this)
        val desired = LauncherConfig(revision = 4, iconOverrides = mapOf(TEST_APP_KEY to iconId))

        val failure = try {
            repository.handleDesired(
                ProtocolVersion.CURRENT,
                PayloadCodec.encodeConfig(desired),
                mapOf(iconId to Asset.createFromBytes(bytes)),
            )
            null
        } catch (e: IOException) {
            e
        }

        assertNotNull(failure)
        assertTrue(store.saves.isEmpty())
        assertTrue(client.puts.isEmpty())
        // The repository is still usable: a later desired applies from the untouched state.
        val ok = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 5)),
            emptyMap(),
        )
        assertTrue(ok.applied)
    }

    @Test
    fun staleDesiredPublishesRejectedStaleReceipt() = runTest {
        val store = FakeSyncStateStore(testPersisted(revision = 5))
        val client = RecordingClient()
        val repository = testRepository(store = store, client = client, scope = this)

        val outcome = repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 4)),
            emptyMap(),
        )

        assertEquals(ApplyResult.REJECTED_STALE, outcome.state.lastResult)
        assertEquals(4L, outcome.state.lastDesiredRevision)
        assertEquals(1, client.statePuts().size)
    }

    @Test
    fun unsupportedSchemaPublishesRejectedSchemaReceipt() = runTest {
        val client = RecordingClient()
        val repository = testRepository(client = client, scope = this)

        val outcome = repository.handleDesired(99, byteArrayOf(1, 2, 3), emptyMap())

        assertEquals(ApplyResult.REJECTED_SCHEMA, outcome.state.lastResult)
        val receipt = (PayloadCodec.decodeState(ProtocolVersion.CURRENT, client.statePuts().single().payload) as DecodeResult.Success).value
        assertEquals(ApplyResult.REJECTED_SCHEMA, receipt.lastResult)
    }

    @Test
    fun malformedPayloadPublishesRejectedInvalidReceipt() = runTest {
        val client = RecordingClient()
        val repository = testRepository(client = client, scope = this)

        val outcome = repository.handleDesired(ProtocolVersion.CURRENT, byteArrayOf(9), emptyMap())

        assertEquals(ApplyResult.REJECTED_INVALID, outcome.state.lastResult)
        assertEquals(1, client.statePuts().size)
    }
}
