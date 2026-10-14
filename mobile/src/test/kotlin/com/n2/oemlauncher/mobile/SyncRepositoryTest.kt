package com.n2.oemlauncher.mobile

import com.n2.oemlauncher.mobile.data.*
import com.n2.oemlauncher.mobile.editor.DraftHistory
import com.n2.oemlauncher.mobile.sync.WearGateway
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.*
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.SyncPhase
import com.n2.oemlauncher.shared.protocol.SyncStatusResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SyncRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val key = "com.example.app/com.example.app.Main"
    private val catalog = WatchCatalog(
        1, "test", 36, "1", WatchDisplay(454, 454, 320, true), listOf(CatalogEntry(key, "App")),
    )
    private class MemoryStore(var snapshot: CompanionSnapshot = CompanionSnapshot()) : SnapshotStore {
        override suspend fun load() = snapshot
        override suspend fun save(snapshot: CompanionSnapshot) { this.snapshot = snapshot }
    }
    private class FakeGateway(
        val fail: Boolean = false,
        val receipt: suspend (LauncherConfig) -> Unit = {},
    ) : WearGateway {
        var published: LauncherConfig? = null
        var publishedIcons: Map<String, ByteArray>? = null
        override suspend fun publish(config: LauncherConfig, icons: Map<String, ByteArray>) {
            if (fail) throw IllegalStateException("offline")
            published = config
            publishedIcons = icons
            receipt(config)
        }
        override suspend fun discover() = WatchConnection(ConnectionKind.CONNECTED)
        override suspend fun requestCatalog() = Unit
        override fun observe(onChanged: () -> Unit) = Unit
        override fun stopObserving() = Unit
        override suspend fun readExisting() = Unit
    }

    @Test fun firstStateSeedsDraftAndSurvivesStorage() = runTest {
        val store = MemoryStore()
        val repo = CompanionRepository(store, IconStore(temporary.root), this)
        advanceUntilIdle()
        val watch = WatchConfigState(LauncherConfig(revision = 9, favorites = listOf(key)))
        repo.receiveState(watch)
        assertEquals(watch.current, repo.state.value.config)
        assertEquals(9L, store.snapshot.draft?.baseRevision)
    }

    @Test fun publishWaitsForReceiptAndAcknowledges() = runTest {
        val store = MemoryStore()
        val repo = CompanionRepository(store, IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 5)))
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        repo.toggleFavorite(key)
        val gateway = FakeGateway()
        repo.sync(gateway)
        val sent = requireNotNull(gateway.published)
        assertEquals(6L, sent.revision)
        assertEquals(SyncPhase.WAITING_FOR_WATCH, repo.state.value.phase)
        assertEquals(sent, store.snapshot.sent)
        repo.receiveState(WatchConfigState(sent, sent.revision, ApplyResult.APPLIED, updatedAtMillis = 100))
        assertEquals(SyncPhase.APPLIED, repo.state.value.phase)
        assertFalse(repo.state.value.conflict)
        assertFalse(repo.state.value.draft!!.dirty)
        assertEquals(100L, repo.state.value.snapshot.lastSyncMillis)
    }

    @Test fun newerWatchRequiresExplicitOverwrite() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 2)))
        repo.toggleFavorite(key)
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 8), lastDesiredRevision = 10))
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        val gateway = FakeGateway()
        repo.sync(gateway)
        assertNull(gateway.published)
        assertTrue(repo.state.value.conflict)
        repo.sync(gateway, overwrite = true)
        assertEquals(11L, gateway.published?.revision)
    }

    @Test fun loadingWatchDiscardsConflictAndKeepsItsConfig() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 2)))
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 3, favorites = listOf(key))))
        repo.loadWatch()
        assertFalse(repo.state.value.conflict)
        assertEquals(listOf(key), repo.state.value.config.favorites)
    }

    @Test fun demoNeverPublishes() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.demo(catalog, LauncherConfig())
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        val gateway = FakeGateway()
        repo.sync(gateway, overwrite = true)
        assertNull(gateway.published)
        assertTrue(repo.state.value.demo)
    }

    @Test fun failedPublishPreservesDraftAndRevisionForRetry() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 3)))
        repo.toggleFavorite(key)
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        repo.sync(FakeGateway(fail = true))
        assertEquals(listOf(key), repo.state.value.config.favorites)
        assertEquals(UserNotice.SYNC_FAILED, repo.state.value.notice)
        assertEquals(SyncPhase.NOT_SENT, repo.state.value.phase)
        assertNull(repo.state.value.snapshot.publishedRevision)
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        val retry = FakeGateway()
        repo.sync(retry)
        assertEquals(5L, retry.published?.revision)
    }

    @Test fun oldReceiptsDoNotRollBackState() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 12)))
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 3)))
        assertEquals(12L, repo.state.value.snapshot.watch?.current?.revision)
    }

    @Test fun everyReceiptPhaseIsRepresented() {
        val expected = mapOf(
            ApplyResult.NONE to SyncPhase.WAITING_FOR_WATCH,
            ApplyResult.APPLIED to SyncPhase.APPLIED,
            ApplyResult.APPLIED_MISSING_ICONS to SyncPhase.APPLIED_MISSING_ICONS,
            ApplyResult.REJECTED_STALE to SyncPhase.CONFLICT,
            ApplyResult.REJECTED_INVALID to SyncPhase.REJECTED,
            ApplyResult.REJECTED_SCHEMA to SyncPhase.WATCH_OUTDATED,
        )
        expected.forEach { (result, phase) ->
            assertEquals(phase, SyncStatusResolver.resolve(4, WatchConfigState(LauncherConfig(), 4, result)))
        }
        assertEquals(SyncPhase.NOT_SENT, SyncStatusResolver.resolve(null, null))
        assertEquals(SyncPhase.WAITING_FOR_WATCH, SyncStatusResolver.resolve(4, WatchConfigState(LauncherConfig(), 3)))
    }
    @Test fun editingBeforeFirstStatePreservesOfflineIntent() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveCatalog(catalog)
        repo.toggleFavorite(key)
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 2)))
        assertEquals(listOf(key), repo.state.value.config.favorites)
        assertTrue(repo.state.value.conflict)
    }
    @Test fun asynchronousLoadDoesNotOverwriteDiscoveryEvents() = runTest {
        val loaded = CompletableDeferred<CompanionSnapshot>()
        val store = object : SnapshotStore {
            override suspend fun load() = loaded.await()
            override suspend fun save(snapshot: CompanionSnapshot) = Unit
        }
        val repo = CompanionRepository(store, IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        loaded.complete(CompanionSnapshot())
        advanceUntilIdle()
        assertEquals(ConnectionKind.CONNECTED, repo.state.value.connection.kind)
        assertFalse(repo.state.value.loading)
    }
    @Test fun missingPlayServicesCannotPublishAndKeepsItsReason() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig()))
        repo.connection(WatchConnection(ConnectionKind.PLAY_SERVICES_MISSING))
        val gateway = FakeGateway()
        repo.sync(gateway)
        assertNull(gateway.published)
        assertEquals(ConnectionKind.PLAY_SERVICES_MISSING, repo.state.value.connection.kind)
    }
    @Test fun receiptCanArriveBeforePublishReturns() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 6)))
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        repo.sync(FakeGateway(receipt = { sent ->
            repo.receiveState(WatchConfigState(sent, sent.revision, ApplyResult.APPLIED))
        }))
        assertEquals(SyncPhase.APPLIED, repo.state.value.phase)
        assertEquals(7L, repo.state.value.snapshot.publishedRevision)
        assertFalse(repo.state.value.conflict)
    }
    @Test fun olderReceiptCannotReplaceNewerOutcomeAtSameWatchRevision() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 6), 10, ApplyResult.REJECTED_STALE))
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 6), 5, ApplyResult.APPLIED))
        assertEquals(10L, repo.state.value.snapshot.watch?.lastDesiredRevision)
        assertEquals(ApplyResult.REJECTED_STALE, repo.state.value.snapshot.watch?.lastResult)
    }
    @Test fun sameRevisionReceiptsAreArbitratedByTimestamp() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 6), 10, ApplyResult.APPLIED, updatedAtMillis = 200))
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 6), 10, ApplyResult.REJECTED_STALE, updatedAtMillis = 100))
        assertEquals(ApplyResult.APPLIED, repo.state.value.snapshot.watch?.lastResult)
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 6), 10, ApplyResult.REJECTED_INVALID, updatedAtMillis = 300))
        assertEquals(ApplyResult.REJECTED_INVALID, repo.state.value.snapshot.watch?.lastResult)
    }
    @Test fun missingLocalAssetsAreSkippedWithoutHidingTheWatch() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveCatalog(catalog)
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 3)))
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        val missingId = IconId.of(byteArrayOf(7, 7, 7))
        repo.edit { ConfigEditor.setIconOverride(it, key, missingId) }
        val gateway = FakeGateway()
        repo.sync(gateway)
        assertNotNull(gateway.published)
        assertTrue(gateway.publishedIcons.isNullOrEmpty())
        assertEquals(ConnectionKind.CONNECTED, repo.state.value.connection.kind)
        assertEquals(UserNotice.ICONS_MISSING, repo.state.value.notice)
        assertEquals(listOf(missingId), repo.state.value.diagnostics.localMissingIcons)
    }
    @Test fun diagnosticsRecordSendOutcomeAndApiErrors() = runTest {
        val repo = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repo.receiveState(WatchConfigState(LauncherConfig(revision = 3)))
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        repo.sync(FakeGateway(fail = true))
        val failed = repo.state.value.diagnostics
        assertEquals(false, failed.lastSendSucceeded)
        assertEquals(4L, failed.lastSentRevision)
        assertEquals("IllegalStateException", failed.lastError?.type)
        repo.connection(WatchConnection(ConnectionKind.CONNECTED))
        repo.sync(FakeGateway())
        val sent = repo.state.value.diagnostics
        assertEquals(true, sent.lastSendSucceeded)
        assertEquals(5L, sent.lastSentRevision)
    }
}
