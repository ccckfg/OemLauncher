package com.n2.oemlauncher.mobile

import com.n2.oemlauncher.mobile.data.CompanionRepository
import com.n2.oemlauncher.mobile.data.CompanionSnapshot
import com.n2.oemlauncher.mobile.data.ConnectionKind
import com.n2.oemlauncher.mobile.data.IconStore
import com.n2.oemlauncher.mobile.data.SnapshotStore
import com.n2.oemlauncher.mobile.data.WatchConnection
import com.n2.oemlauncher.mobile.sync.DesiredItem
import com.n2.oemlauncher.mobile.sync.WearGateway
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.protocol.DecodeResult
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.SyncKeys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The real gateway hands the publish call to [DesiredItem], and this fake records the same call,
 * so asserting on the built DataMap covers the keys the phone actually writes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DesiredItemTest {
    @get:Rule val temporary = TemporaryFolder()
    private val key = "com.example.app/com.example.app.Main"
    private val catalog = WatchCatalog(
        1, "test", 36, "1", WatchDisplay(454, 454, 320, true), listOf(CatalogEntry(key, "App")),
    )

    private class MemoryStore : SnapshotStore {
        override suspend fun load() = CompanionSnapshot()
        override suspend fun save(snapshot: CompanionSnapshot) = Unit
    }

    private class RecordingGateway : WearGateway {
        var published: LauncherConfig? = null
        var publishedIcons: Map<String, ByteArray>? = null
        override suspend fun publish(config: LauncherConfig, icons: Map<String, ByteArray>) {
            published = config
            publishedIcons = icons
        }
        override suspend fun discover() = WatchConnection(ConnectionKind.CONNECTED)
        override suspend fun requestCatalog() = Unit
        override fun observe(onChanged: () -> Unit) = Unit
        override fun stopObserving() = Unit
        override suspend fun readExisting() = Unit
    }

    @Test fun publishedDesiredCarriesTheRevisionOutsideThePayload() = runTest {
        val repository = CompanionRepository(MemoryStore(), IconStore(temporary.root), this)
        advanceUntilIdle()
        repository.receiveCatalog(catalog)
        repository.receiveState(WatchConfigState(LauncherConfig(revision = 41)))
        repository.connection(WatchConnection(ConnectionKind.CONNECTED))
        val iconBytes = byteArrayOf(1, 2, 3, 4, 5)
        val iconId = repository.icons.put(iconBytes)
        repository.edit { ConfigEditor.setIconOverride(it, key, iconId) }
        val gateway = RecordingGateway()
        repository.sync(gateway)

        val sent = requireNotNull(gateway.published)
        assertEquals(42L, sent.revision)
        val map = DesiredItem.request(sent, requireNotNull(gateway.publishedIcons)).dataMap
        assertEquals(sent.revision, map.getLong(SyncKeys.REVISION))
        assertEquals(ProtocolVersion.CURRENT, map.getInt(SyncKeys.SCHEMA))
        assertNotNull(map.getAsset(SyncKeys.iconAssetKey(iconId)))
        val payload = requireNotNull(map.getByteArray(SyncKeys.PAYLOAD))
        val decoded = PayloadCodec.decodeConfig(map.getInt(SyncKeys.SCHEMA), payload)
        assertTrue(decoded is DecodeResult.Success)
        assertEquals(sent, (decoded as DecodeResult.Success).value)
    }
}
