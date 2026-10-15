package com.n2.oemlauncher.wear.sync

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.PayloadCodec
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import com.n2.oemlauncher.shared.protocol.ProtocolVersion
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesiredProcessorTest {
    private val icons = FakeIconStore()
    private val processor = DesiredProcessor(icons)

    @Test
    fun decideRejectsUnsupportedSchemaAndMalformedPayload() {
        val state = testState(revision = 0)
        val schema = processor.decide(99, byteArrayOf(), null, state, 1L) as DesiredDecision.Rejected
        assertEquals(ApplyResult.REJECTED_SCHEMA, schema.state.lastResult)
        val malformed = processor.decide(ProtocolVersion.CURRENT, byteArrayOf(1), null, state, 1L) as DesiredDecision.Rejected
        assertEquals(ApplyResult.REJECTED_INVALID, malformed.state.lastResult)
    }

    @Test
    fun decideIgnoresRedeliveryAndRejectsStale() {
        val state = testState(revision = 4, lastDesired = 4)
        val redelivery = processor.decide(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 4)),
            null,
            state,
            1L,
        )
        assertEquals(DesiredDecision.Redelivery, redelivery)

        val stale = processor.decide(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 3)),
            null,
            testState(revision = 4),
            1L,
        ) as DesiredDecision.Rejected
        assertEquals(ApplyResult.REJECTED_STALE, stale.state.lastResult)
        assertEquals(3L, stale.state.lastDesiredRevision)
    }

    @Test
    fun decideAcceptsNewerRevisionAndListsReferencedIcons() {
        val iconId = IconId.of("a".encodeToByteArray())
        val desired = LauncherConfig(revision = 6, iconOverrides = mapOf(TEST_APP_KEY to iconId))
        val decision = processor.decide(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(desired),
            null,
            testState(revision = 5),
            1L,
        ) as DesiredDecision.Apply
        assertEquals(listOf(iconId), decision.referencedIconIds)
        assertEquals(6L, decision.desired.revision)
    }

    @Test
    fun rejectionsAdoptTheReportedRevisionWhenTheKeyIsPresent() {
        val state = testState(revision = 5, lastDesired = 5)

        val schema = processor.decide(99, null, 9L, state, 1L) as DesiredDecision.Rejected
        assertEquals(ApplyResult.REJECTED_SCHEMA, schema.state.lastResult)
        assertEquals(9L, schema.state.lastDesiredRevision)

        val missingPayload = processor.decide(ProtocolVersion.CURRENT, null, 9L, state, 1L) as DesiredDecision.Rejected
        assertEquals(ApplyResult.REJECTED_INVALID, missingPayload.state.lastResult)
        assertEquals(9L, missingPayload.state.lastDesiredRevision)

        val malformed = processor.decide(ProtocolVersion.CURRENT, byteArrayOf(1), 9L, state, 1L) as DesiredDecision.Rejected
        assertEquals(ApplyResult.REJECTED_INVALID, malformed.state.lastResult)
        assertEquals(9L, malformed.state.lastDesiredRevision)
    }

    @Test
    fun rejectionsKeepTheRevisionWhenTheKeyIsMissing() {
        val state = testState(revision = 5, lastDesired = 3)

        val schema = processor.decide(99, null, null, state, 1L) as DesiredDecision.Rejected
        assertEquals(3L, schema.state.lastDesiredRevision)

        val missingPayload = processor.decide(ProtocolVersion.CURRENT, null, null, state, 1L) as DesiredDecision.Rejected
        assertEquals(3L, missingPayload.state.lastDesiredRevision)

        val malformed = processor.decide(ProtocolVersion.CURRENT, byteArrayOf(1), null, state, 1L) as DesiredDecision.Rejected
        assertEquals(3L, malformed.state.lastDesiredRevision)
    }

    @Test
    fun rejectionsNeverLowerTheLastDesiredRevision() {
        val state = testState(revision = 5, lastDesired = 7)
        val rejected = processor.decide(99, null, 3L, state, 1L) as DesiredDecision.Rejected
        assertEquals(7L, rejected.state.lastDesiredRevision)
    }

    @Test
    fun redeliveryIsUnchangedByTheReportedRevision() {
        val state = testState(revision = 4, lastDesired = 4)
        val decision = processor.decide(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 4)),
            9L,
            state,
            1L,
        )
        assertEquals(DesiredDecision.Redelivery, decision)
    }

    @Test
    fun decodablePayloadKeepsItsOwnRevisionOverTheReportedOne() {
        val applied = processor.decide(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 6)),
            9L,
            testState(revision = 5),
            1L,
        ) as DesiredDecision.Apply
        assertEquals(6L, applied.desired.revision)

        val stale = processor.decide(
            ProtocolVersion.CURRENT,
            PayloadCodec.encodeConfig(LauncherConfig(revision = 5)),
            9L,
            testState(revision = 5, lastDesired = 2),
            1L,
        ) as DesiredDecision.Rejected
        assertEquals(ApplyResult.REJECTED_STALE, stale.state.lastResult)
        assertEquals(5L, stale.state.lastDesiredRevision)
    }

    @Test
    fun applyStoresValidIconsAndReportsInvalidOnes() {
        val good = "good".encodeToByteArray()
        val goodId = IconId.of(good)
        val mismatched = "mismatched".encodeToByteArray()
        val oversized = ByteArray(ProtocolLimits.MAX_ICON_BYTES + 1) { 7 }
        val missingId = IconId.of("missing".encodeToByteArray())
        val desired = LauncherConfig(
            revision = 2,
            iconOverrides = mapOf(
                "com.example.app/com.example.app.One" to goodId,
                "com.example.app/com.example.app.Two" to IconId.of(mismatched),
                "com.example.app/com.example.app.Three" to IconId.of(oversized),
                "com.example.app/com.example.app.Four" to missingId,
            ),
        )
        val outcome = processor.apply(
            desired,
            mapOf(
                goodId to good,
                IconId.of(mismatched) to mismatched + "x".encodeToByteArray(),
                IconId.of(oversized) to oversized,
            ),
            5L,
        )
        assertArrayEquals(good, icons.stored[goodId])
        assertEquals(ApplyResult.APPLIED_MISSING_ICONS, outcome.state.lastResult)
        assertEquals(3, outcome.state.missingIconIds.size)
        assertFalse(outcome.state.missingIconIds.contains(goodId))
        assertEquals(5L, outcome.state.updatedAtMillis)
        assertEquals(2L, outcome.state.lastDesiredRevision)
    }

    @Test
    fun applyReusesIconAlreadyStoredOnDisk() {
        val bytes = "stored".encodeToByteArray()
        val iconId = IconId.of(bytes)
        icons.stored[iconId] = bytes
        val desired = LauncherConfig(revision = 3, iconOverrides = mapOf(TEST_APP_KEY to iconId))
        val outcome = processor.apply(desired, emptyMap(), 1L)
        assertEquals(ApplyResult.APPLIED, outcome.state.lastResult)
        assertTrue(outcome.state.missingIconIds.isEmpty())
    }

    @Test
    fun applyTreatsWrongStoredContentAsMissing() {
        val iconId = IconId.of("wanted".encodeToByteArray())
        icons.stored[iconId] = "something else".encodeToByteArray()
        val desired = LauncherConfig(revision = 3, iconOverrides = mapOf(TEST_APP_KEY to iconId))
        val outcome = processor.apply(desired, emptyMap(), 1L)
        assertEquals(ApplyResult.APPLIED_MISSING_ICONS, outcome.state.lastResult)
        assertEquals(listOf(iconId), outcome.state.missingIconIds)
    }
}
