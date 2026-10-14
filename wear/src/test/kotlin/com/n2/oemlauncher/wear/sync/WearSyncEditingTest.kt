package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.ConfigOrigin
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WearSyncEditingTest {

    @Test
    fun localEditNumbersRevisionAboveConfigAndDesired() = runTest {
        val store = FakeSyncStateStore(testPersisted(revision = 3, lastDesired = 7))
        val client = RecordingClient()
        val repository = testRepository(store = store, client = client, scope = this, clock = { 42L })

        val edited = repository.localEdit { it.copy(sortMode = SortMode.ALPHABETICAL) }

        assertEquals(8L, edited.revision)
        assertEquals(ConfigOrigin.WATCH, edited.origin)
        assertEquals(42L, edited.editedAtMillis)
        assertEquals(SortMode.ALPHABETICAL, edited.sortMode)
        assertEquals(8L, store.persisted.state?.current?.revision)
        assertEquals(1, client.statePuts().size)
    }

    @Test
    fun localEditAfterAppliedDesiredContinuesTheRevision() = runTest {
        val repository = testRepository(scope = this)
        repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 5)),
            emptyMap(),
        )

        val edited = repository.localEdit { it }

        assertEquals(6L, edited.revision)
    }

    @Test
    fun staleLocalEditKeepsTheNewerState() = runTest {
        val store = FakeSyncStateStore(testPersisted(revision = 10, lastDesired = 10))
        val repository = testRepository(store = store, scope = this)

        repository.publishState(LauncherConfig(revision = 4))

        assertEquals(10L, store.persisted.state?.current?.revision)
    }

    @Test
    fun failedReceiptStaysDirtyAndIsResentAfterRecovery() = runTest {
        val client = RecordingClient().apply { failPuts = true }
        val store = FakeSyncStateStore()
        val repository = testRepository(store = store, client = client, scope = this)

        repository.publishState(LauncherConfig(revision = 1))

        assertEquals(listOf(true), store.saves.map { it.second })
        assertEquals(false, repository.status.value.connected)

        client.failPuts = false
        repository.retryPending()

        assertEquals(1, client.statePuts().size)
        assertEquals(false, store.saves.last().second)
        assertEquals(true, repository.status.value.connected)
    }

    @Test
    fun identicalStateIsNotPublishedTwice() = runTest {
        val client = RecordingClient()
        val repository = testRepository(client = client, scope = this)
        val config = LauncherConfig(revision = 1)

        repository.publishState(config)
        repository.publishState(config)

        assertEquals(1, client.statePuts().size)
    }

    @Test
    fun failedSendDoesNotRecordTheStateDigest() = runTest {
        val client = RecordingClient().apply { failPuts = true }
        val repository = testRepository(client = client, scope = this)
        val config = LauncherConfig(revision = 1)

        repository.publishState(config)
        assertTrue(client.puts.isEmpty())

        client.failPuts = false
        repository.publishState(config)

        assertEquals(1, client.statePuts().size)
    }

    @Test
    fun dirtyStateFromPreviousRunIsResentOnRestore() = runTest {
        val store = FakeSyncStateStore(testPersisted(revision = 6, lastDesired = 6, dirty = true))
        val client = RecordingClient()
        val repository = testRepository(store = store, client = client, scope = this)

        repository.retryPending()

        assertEquals(1, client.statePuts().size)
        assertEquals(false, store.saves.last().second)
    }

    @Test
    fun statusTracksResultConnectionAndSyncTime() = runTest {
        val client = RecordingClient()
        val repository = testRepository(client = client, scope = this, clock = { 100L })
        assertNull(repository.status.value.connected)

        repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 1)),
            emptyMap(),
        )

        assertEquals(true, repository.status.value.connected)
        assertEquals(100L, repository.status.value.lastSyncMillis)
        assertEquals(ApplyResult.APPLIED, repository.status.value.lastResult)

        client.failPuts = true
        repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 2)),
            emptyMap(),
        )
        assertEquals(false, repository.status.value.connected)
    }

    @Test
    fun notifiesTheTileRefresherAfterAnAppliedChange() = runTest {
        val notified = mutableListOf<Long>()
        val repository = testRepository(scope = this, onStateChanged = { notified += it.current.revision })

        repository.handleDesired(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 3)),
            emptyMap(),
        )
        runCurrent()

        assertEquals(listOf(3L), notified)
    }
}
