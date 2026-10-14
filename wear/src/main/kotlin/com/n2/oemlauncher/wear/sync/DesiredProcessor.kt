package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.DecodeResult
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import com.n2.oemlauncher.shared.protocol.RevisionPolicy
import com.n2.oemlauncher.wear.data.ConfigWriter
import com.n2.oemlauncher.wear.data.IconFileStore

data class DesiredOutcome(val state: WatchConfigState, val applied: Boolean)

class DesiredProcessor(
    private val configRepository: ConfigWriter,
    private val iconStore: IconFileStore,
) {
    suspend fun process(
        schema: Int,
        payload: ByteArray,
        assets: Map<String, ByteArray>,
        currentState: WatchConfigState,
        now: Long,
    ): DesiredOutcome {
        if (!ProtocolVersion.isSupported(schema)) return outcome(currentState, ApplyResult.REJECTED_SCHEMA, now)
        val decoded = PayloadCodec.decodeConfig(schema, payload)
        if (decoded !is DecodeResult.Success) return outcome(currentState, ApplyResult.REJECTED_INVALID, now)
        val desired = decoded.value
        if (RevisionPolicy.isRedelivery(desired.revision, currentState)) return DesiredOutcome(currentState, false)
        if (!RevisionPolicy.shouldApply(desired.revision, currentState)) {
            return outcome(currentState, ApplyResult.REJECTED_STALE, now, desired.revision)
        }
        val missing = desired.iconOverrides.values.distinct().filter { iconId ->
            val bytes = assets[iconId]
            if (bytes == null || bytes.size > com.n2.oemlauncher.shared.protocol.ProtocolLimits.MAX_ICON_BYTES || IconId.of(bytes) != iconId) {
                true
            } else {
                iconStore.put(iconId, bytes)
                false
            }
        }
        configRepository.save(desired)
        val result = if (missing.isEmpty()) ApplyResult.APPLIED else ApplyResult.APPLIED_MISSING_ICONS
        return outcome(currentState.copy(current = desired), result, now, desired.revision, missing, applied = true)
    }

    private fun outcome(
        state: WatchConfigState,
        result: ApplyResult,
        now: Long,
        desiredRevision: Long = state.lastDesiredRevision,
        missing: List<String> = emptyList(),
        applied: Boolean = false,
    ) = DesiredOutcome(state.copy(lastDesiredRevision = desiredRevision, lastResult = result, missingIconIds = missing, updatedAtMillis = now), applied)
}
