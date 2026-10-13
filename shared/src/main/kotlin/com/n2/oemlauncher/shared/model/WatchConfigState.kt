package com.n2.oemlauncher.shared.model

import kotlinx.serialization.Serializable

/** Outcome of the last desired config the watch processed. */
@Serializable
enum class ApplyResult {
    NONE,
    APPLIED,

    /** Applied, but some custom icons were unavailable; defaults are shown for those apps. */
    APPLIED_MISSING_ICONS,

    /** The watch already shows an equal or newer revision (usually an edit made on the watch). */
    REJECTED_STALE,
    REJECTED_INVALID,

    /** The phone speaks a protocol schema this watch build does not support. */
    REJECTED_SCHEMA,
}

/**
 * Watch-owned state published after every change: the config it currently shows plus the
 * result of the last desired config it processed. This doubles as the apply acknowledgement.
 */
@Serializable
data class WatchConfigState(
    val current: LauncherConfig,
    val lastDesiredRevision: Long = 0,
    val lastResult: ApplyResult = ApplyResult.NONE,
    val missingIconIds: List<String> = emptyList(),
    val updatedAtMillis: Long = 0,
)
