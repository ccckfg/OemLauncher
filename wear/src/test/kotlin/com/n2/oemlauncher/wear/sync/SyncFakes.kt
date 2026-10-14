package com.n2.oemlauncher.wear.sync

import com.google.android.gms.wearable.Asset
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.SyncPaths
import com.n2.oemlauncher.wear.data.IconStore
import com.n2.oemlauncher.wear.data.PersistedSyncState
import com.n2.oemlauncher.wear.data.SyncStateStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope

/** Canonical app key used by the sync tests. */
const val TEST_APP_KEY = "com.example.app/com.example.app.MainActivity"

/** In-memory [IconStore] with an optional write failure, so mid-way failures are testable. */
class FakeIconStore : IconStore {
    val stored = LinkedHashMap<String, ByteArray>()
    var failOnPut: String? = null

    override fun put(iconId: String, bytes: ByteArray) {
        if (iconId == failOnPut) throw IOException("simulated icon write failure")
        stored[iconId] = bytes
    }

    override fun get(iconId: String): ByteArray? = stored[iconId]

    override fun isValidStored(iconId: String): Boolean =
        stored[iconId]?.let { IconId.of(it) == iconId } ?: false
}

/** In-memory [SyncStateStore] that also serves as the durable state between two repository runs. */
class FakeSyncStateStore(
    var persisted: PersistedSyncState = PersistedSyncState(null, null, false),
) : SyncStateStore {
    val saves = mutableListOf<Pair<WatchConfigState, Boolean>>()

    override suspend fun load(): PersistedSyncState = persisted

    override suspend fun save(state: WatchConfigState, dirty: Boolean) {
        saves += state to dirty
        persisted = PersistedSyncState(state, state.current, dirty)
    }
}

data class PutCall(
    val path: String,
    val schema: Int,
    val payload: ByteArray,
    val assets: Map<String, Asset>,
    val urgent: Boolean,
)

/** Records Data Layer traffic and can simulate a link that is down. */
class RecordingClient : DataLayerClient {
    val puts = mutableListOf<PutCall>()
    val assetReads = mutableListOf<Asset>()
    val assetBytes = HashMap<Asset, ByteArray>()
    var failPuts = false

    override suspend fun put(
        path: String,
        schema: Int,
        payload: ByteArray,
        assets: Map<String, Asset>,
        urgent: Boolean,
    ): Boolean {
        if (failPuts) return false
        puts += PutCall(path, schema, payload, assets, urgent)
        return true
    }

    override suspend fun send(path: String): Boolean = true

    override suspend fun readAsset(asset: Asset): ByteArray? {
        assetReads += asset
        return assetBytes[asset]
    }

    fun statePuts(): List<PutCall> = puts.filter { it.path == SyncPaths.CONFIG_STATE }

    fun catalogPuts(): List<PutCall> = puts.filter { it.path == SyncPaths.CATALOG }
}

fun testCatalog(stamp: Long = 1L): WatchCatalog =
    WatchCatalog(stamp, "watch", 36, "1.0", WatchDisplay(454, 454, 320, true), emptyList())

fun testState(revision: Long, lastDesired: Long = 0L): WatchConfigState =
    WatchConfigState(LauncherConfig(revision = revision), lastDesiredRevision = lastDesired)

fun testPersisted(revision: Long, lastDesired: Long = 0L, dirty: Boolean = false): PersistedSyncState =
    PersistedSyncState(testState(revision, lastDesired), LauncherConfig(revision = revision), dirty)

fun testRepository(
    store: FakeSyncStateStore = FakeSyncStateStore(),
    client: RecordingClient = RecordingClient(),
    icons: FakeIconStore = FakeIconStore(),
    scope: CoroutineScope,
    provider: CatalogProvider = CatalogProvider { CatalogBundle(testCatalog(), emptyMap()) },
    clock: () -> Long = { 1_000L },
    onStateChanged: (WatchConfigState) -> Unit = {},
): WearSyncRepository = WearSyncRepository(
    store = store,
    iconStore = icons,
    client = client,
    catalogProvider = provider,
    scope = scope,
    onStateChanged = onStateChanged,
    clock = clock,
)
