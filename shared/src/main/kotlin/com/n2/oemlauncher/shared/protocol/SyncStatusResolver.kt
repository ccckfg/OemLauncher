package com.n2.oemlauncher.shared.protocol

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.WatchConfigState

/** What the phone shows for the config it sent last. */
enum class SyncPhase {
    NOT_SENT,
    WAITING_FOR_WATCH,
    APPLIED,
    APPLIED_MISSING_ICONS,

    /** The watch kept an equal or newer revision, usually an edit made on the watch itself. */
    CONFLICT,
    REJECTED,

    /** The watch app is older than the phone app and cannot read the payload. */
    WATCH_OUTDATED,
}

object SyncStatusResolver {

    /**
     * [sentRevision] is the revision of the phone's last published desired config (null when
     * nothing was sent); [state] is the latest WatchConfigState received from the watch.
     */
    fun resolve(sentRevision: Long?, state: WatchConfigState?): SyncPhase {
        if (sentRevision == null) return SyncPhase.NOT_SENT
        if (state == null || state.lastDesiredRevision < sentRevision) {
            return SyncPhase.WAITING_FOR_WATCH
        }
        return when (state.lastResult) {
            ApplyResult.NONE -> SyncPhase.WAITING_FOR_WATCH
            ApplyResult.APPLIED -> SyncPhase.APPLIED
            ApplyResult.APPLIED_MISSING_ICONS -> SyncPhase.APPLIED_MISSING_ICONS
            ApplyResult.REJECTED_STALE -> SyncPhase.CONFLICT
            ApplyResult.REJECTED_INVALID -> SyncPhase.REJECTED
            ApplyResult.REJECTED_SCHEMA -> SyncPhase.WATCH_OUTDATED
        }
    }

    /** True when the watch shows edits made on the watch after the phone's draft was based. */
    fun watchHasNewerEdits(draftBaseRevision: Long, state: WatchConfigState?): Boolean =
        state != null && state.current.revision > draftBaseRevision
}
