package com.n2.oemlauncher.shared.protocol

import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRulesTest {

    private fun state(current: Long, lastDesired: Long, result: ApplyResult = ApplyResult.APPLIED) =
        WatchConfigState(current = LauncherConfig(revision = current), lastDesiredRevision = lastDesired, lastResult = result)

    @Test
    fun nextRevisionExceedsEverythingSeen() {
        assertEquals(1L, RevisionPolicy.next())
        assertEquals(8L, RevisionPolicy.next(3, 7, -2))
    }

    @Test
    fun watchAppliesOnlyNewerDesiredConfigs() {
        assertTrue(RevisionPolicy.shouldApply(5, state(current = 4, lastDesired = 4)))
        assertFalse("stale vs watch edit", RevisionPolicy.shouldApply(5, state(current = 5, lastDesired = 2)))
        assertFalse("re-delivery", RevisionPolicy.shouldApply(5, state(current = 1, lastDesired = 5)))
        assertTrue(RevisionPolicy.isRedelivery(5, state(current = 1, lastDesired = 5)))
    }

    @Test
    fun phoneStatusFollowsWatchState() {
        assertEquals(SyncPhase.NOT_SENT, SyncStatusResolver.resolve(null, null))
        assertEquals(SyncPhase.WAITING_FOR_WATCH, SyncStatusResolver.resolve(6, null))
        assertEquals(SyncPhase.WAITING_FOR_WATCH, SyncStatusResolver.resolve(6, state(5, 5)))
        assertEquals(SyncPhase.APPLIED, SyncStatusResolver.resolve(6, state(6, 6)))
        assertEquals(SyncPhase.CONFLICT, SyncStatusResolver.resolve(6, state(6, 6, ApplyResult.REJECTED_STALE)))
        assertEquals(SyncPhase.WATCH_OUTDATED, SyncStatusResolver.resolve(6, state(2, 6, ApplyResult.REJECTED_SCHEMA)))
        assertTrue(SyncStatusResolver.watchHasNewerEdits(draftBaseRevision = 4, state = state(5, 3)))
        assertFalse(SyncStatusResolver.watchHasNewerEdits(draftBaseRevision = 5, state = state(5, 3)))
    }

    @Test
    fun iconIdsAreContentAddressed() {
        val id = IconId.of(byteArrayOf(1, 2, 3))
        assertEquals(IconId.LENGTH, id.length)
        assertTrue(IconId.isValid(id))
        assertEquals(id, IconId.of(byteArrayOf(1, 2, 3)))
        assertFalse(id == IconId.of(byteArrayOf(3, 2, 1)))
        assertFalse(IconId.isValid(id.uppercase()))
        assertFalse(IconId.isValid(id.dropLast(1)))
    }

    @Test
    fun iconAssetKeysRoundTrip() {
        val id = IconId.of(byteArrayOf(9))
        assertEquals(id, SyncKeys.iconIdFromAssetKey(SyncKeys.iconAssetKey(id)))
        assertNull(SyncKeys.iconIdFromAssetKey(SyncKeys.PAYLOAD))
        assertNull(SyncKeys.iconIdFromAssetKey("icon_nothex"))
    }
}
