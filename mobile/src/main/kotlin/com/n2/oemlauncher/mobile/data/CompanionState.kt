package com.n2.oemlauncher.mobile.data

import com.n2.oemlauncher.mobile.editor.DraftHistory
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.SyncStatusResolver
import kotlinx.serialization.Serializable

enum class ConnectionKind { UNDISCOVERED, CONNECTED, APP_MISSING, PLAY_SERVICES_MISSING }

/** One discovered node and whether it advertises the watch app capability. */
data class NodeStatus(val name: String, val hasWatchApp: Boolean)

data class WatchConnection(
    val kind: ConnectionKind = ConnectionKind.UNDISCOVERED,
    val nodes: List<NodeStatus> = emptyList(),
)

enum class UserNotice { NONE, FAVORITE_LIMIT, ICON_LIMIT, IMPORT_FAILED, ICON_FAILED, SYNC_FAILED, DATA_INVALID, ICONS_MISSING }

/**
 * Kind and time only, never payloads or node identifiers. [statusCode] is the ApiException
 * status code, or null when the failure was not an API call (the UI maps it to a phrase).
 */
data class ApiError(val statusCode: Int?, val atMillis: Long)

/** Session-scoped sync facts for the diagnostics page; not persisted. */
data class SyncDiagnostics(
    val nodes: List<NodeStatus> = emptyList(),
    val lastCatalogMillis: Long = 0,
    val lastStateMillis: Long = 0,
    val lastSentRevision: Long? = null,
    val lastSendSucceeded: Boolean? = null,
    val localMissingIcons: List<String> = emptyList(),
    val lastError: ApiError? = null,
)

@Serializable
data class CompanionSnapshot(
    val catalog: WatchCatalog? = null,
    val watch: WatchConfigState? = null,
    val draft: DraftHistory? = null,
    val sent: LauncherConfig? = null,
    val publishedRevision: Long? = null,
    val lastSyncMillis: Long = 0,
    val demo: Boolean = false,
)

data class CompanionState(
    val snapshot: CompanionSnapshot = CompanionSnapshot(),
    val connection: WatchConnection = WatchConnection(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val notice: UserNotice = UserNotice.NONE,
    val iconGeneration: Long = 0,
    val diagnostics: SyncDiagnostics = SyncDiagnostics(),
) {
    val catalog get() = snapshot.catalog
    val draft get() = snapshot.draft
    val config get() = draft?.current ?: LauncherConfig()
    val demo get() = snapshot.demo
    val phase get() = SyncStatusResolver.resolve(snapshot.publishedRevision, snapshot.watch)
    val conflict get() = !demo && SyncStatusResolver.watchHasNewerEdits(
        draft?.baseRevision ?: 0, snapshot.watch,
    )
    val canSync get() = !demo && draft != null && !busy &&
        connection.kind == ConnectionKind.CONNECTED && snapshot.watch != null
}
