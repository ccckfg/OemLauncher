package com.n2.oemlauncher.wear.ui

import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.wear.config.WearUiTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherEntranceTest {
    @Test fun everyCellStartsHiddenAndEndsSettled() {
        for (reach in listOf(0f, 0.5f, 1f, 1.4f)) {
            assertEquals(0f, LauncherEntrance.cellProgress(0f, reach), 0f)
            assertEquals(1f, LauncherEntrance.cellProgress(1f, reach), 0f)
        }
    }

    @Test fun centerLeadsAndTheRimFollows() {
        val center = LauncherEntrance.cellProgress(0.3f, 0f)
        val middle = LauncherEntrance.cellProgress(0.3f, 0.5f)
        val rim = LauncherEntrance.cellProgress(0.3f, 1f)
        assertTrue(center > middle && middle > rim)
        assertEquals(0f, rim, 0f)
    }

    @Test fun centerSettlesBeforeTheTimelineEnds() {
        assertEquals(1f, LauncherEntrance.cellProgress(1f - WearUiTokens.ENTRANCE_STAGGER, 0f), 1e-4f)
    }

    @Test fun cellsGrowAndSpreadIntoTheirRestingPlace() {
        assertEquals(WearUiTokens.ENTRANCE_START_SCALE, LauncherEntrance.cellScale(0f), 0f)
        assertEquals(WearUiTokens.ENTRANCE_START_SPREAD, LauncherEntrance.cellSpread(0f), 0f)
        assertEquals(1f, LauncherEntrance.cellScale(1f), 0f)
        assertEquals(1f, LauncherEntrance.cellSpread(1f), 0f)
        assertEquals(1f, LauncherEntrance.pageScale(1f), 1e-4f)
    }

    @Test fun styleFollowsTierMotionAndLayout() {
        val refined = LayoutSettings(visualTier = VisualTier.REFINED, reduceMotion = false)
        assertEquals(EntranceStyle.BLOOM, EntranceStyle.of(refined.copy(mode = LayoutMode.HONEYCOMB)))
        assertEquals(EntranceStyle.PAGE, EntranceStyle.of(refined.copy(mode = LayoutMode.GRID)))
        assertEquals(EntranceStyle.FADE, EntranceStyle.of(refined.copy(visualTier = VisualTier.LITE)))
        assertEquals(EntranceStyle.FADE, EntranceStyle.of(refined.copy(reduceMotion = true)))
        assertTrue(EntranceStyle.FADE.durationMs <= WearUiTokens.MOTION_FADE_MAX_MS)
    }
}
