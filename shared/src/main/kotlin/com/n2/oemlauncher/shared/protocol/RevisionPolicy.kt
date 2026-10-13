package com.n2.oemlauncher.shared.protocol

import com.n2.oemlauncher.shared.model.WatchConfigState

/**
 * Last-writer-wins by revision. Both devices number every committed edit strictly above every
 * revision they have seen, so the higher revision is always the newer intent.
 */
object RevisionPolicy {

    /** Revision for a new commit, given every revision this device knows about. */
    fun next(vararg seen: Long): Long = (seen.maxOrNull() ?: 0L).coerceAtLeast(0L) + 1

    /**
     * The watch applies a desired config only when it is newer than what the watch shows and
     * newer than any desired config it already processed (re-deliveries are ignored).
     */
    fun shouldApply(desiredRevision: Long, state: WatchConfigState): Boolean =
        desiredRevision > state.current.revision && desiredRevision > state.lastDesiredRevision

    /** True when a desired config was already processed and must not be applied again. */
    fun isRedelivery(desiredRevision: Long, state: WatchConfigState): Boolean =
        desiredRevision <= state.lastDesiredRevision
}
