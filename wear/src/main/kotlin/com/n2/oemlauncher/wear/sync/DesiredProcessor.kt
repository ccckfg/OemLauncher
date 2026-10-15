package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.DecodeResult
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.RevisionPolicy
import com.n2.oemlauncher.wear.data.IconStore

/** Outcome of one desired config the watch processed; [applied] tells whether the config changed. */
data class DesiredOutcome(val state: WatchConfigState, val applied: Boolean)

/** What the watch decided to do with a desired config, before any asset is downloaded. */
sealed class DesiredDecision {
    /** Rejected before any change; [state] carries the updated lastResult for the receipt. */
    data class Rejected(val state: WatchConfigState) : DesiredDecision()

    /** Already-processed revision: ignore it and leave the published state untouched. */
    object Redelivery : DesiredDecision()

    /** Newer config: read [referencedIconIds], then call [DesiredProcessor.apply]. */
    data class Apply(val desired: LauncherConfig, val referencedIconIds: List<String>) : DesiredDecision()
}

/**
 * Steps 1-5 of "手表处理 desired 的步骤" in docs/sync-protocol.md. [decide] runs before any IO
 * and answers what should happen; [apply] validates the downloaded icons, stores them, and
 * reports the result. Persisting the config and publishing the receipt is the repository's job,
 * so a failure between the two never leaves a half-applied configuration.
 */
class DesiredProcessor(private val iconStore: IconStore) {

    fun decide(
        schema: Int,
        payload: ByteArray?,
        reportedRevision: Long?,
        currentState: WatchConfigState,
        now: Long,
    ): DesiredDecision {
        if (!ProtocolVersion.isSupported(schema)) {
            return DesiredDecision.Rejected(rejected(currentState, ApplyResult.REJECTED_SCHEMA, now, reportedRevision))
        }
        val decoded = payload?.let { PayloadCodec.decodeConfig(schema, it) }
        if (decoded !is DecodeResult.Success) {
            // Covers both a malformed payload and a missing one; the phone still needs a receipt.
            return DesiredDecision.Rejected(rejected(currentState, ApplyResult.REJECTED_INVALID, now, reportedRevision))
        }
        val desired = decoded.value
        if (RevisionPolicy.isRedelivery(desired.revision, currentState)) return DesiredDecision.Redelivery
        if (!RevisionPolicy.shouldApply(desired.revision, currentState)) {
            return DesiredDecision.Rejected(rejected(currentState, ApplyResult.REJECTED_STALE, now, desired.revision))
        }
        return DesiredDecision.Apply(desired, desired.iconOverrides.values.distinct())
    }

    /**
     * Validates the fetched icon bytes and stores them before reporting success. Icons that
     * were not fetched but are already on disk with a matching digest are reused, not reported
     * as missing: identical content is only ever transferred once.
     */
    fun apply(desired: LauncherConfig, assets: Map<String, ByteArray>, now: Long): DesiredOutcome {
        val referenced = desired.iconOverrides.values.distinct()
        val missing = referenced.filter { id ->
            val bytes = assets[id]
            when {
                bytes != null && isValid(id, bytes) -> {
                    iconStore.put(id, bytes)
                    false
                }
                bytes != null -> true
                else -> !iconStore.isValidStored(id)
            }
        }
        val result = if (missing.isEmpty()) ApplyResult.APPLIED else ApplyResult.APPLIED_MISSING_ICONS
        return DesiredOutcome(
            WatchConfigState(
                current = desired,
                lastDesiredRevision = desired.revision,
                lastResult = result,
                missingIconIds = missing,
                updatedAtMillis = now,
            ),
            applied = true,
        )
    }

    private fun isValid(iconId: String, bytes: ByteArray): Boolean =
        bytes.size <= ProtocolLimits.MAX_ICON_BYTES && IconId.of(bytes) == iconId

    /**
     * Rejection receipt. [reportedRevision] is the revision the phone declared for this desired:
     * the payload revision when it decoded, otherwise the DataMap `SyncKeys.REVISION` value (null
     * when the phone is too old to send it). It only ever raises [WatchConfigState.lastDesiredRevision],
     * so a rejection can never roll the receipt history back.
     */
    private fun rejected(
        state: WatchConfigState,
        result: ApplyResult,
        now: Long,
        reportedRevision: Long? = null,
    ): WatchConfigState = state.copy(
        lastDesiredRevision = reportedRevision?.let { maxOf(state.lastDesiredRevision, it) }
            ?: state.lastDesiredRevision,
        lastResult = result,
        missingIconIds = emptyList(),
        updatedAtMillis = now,
    )
}
