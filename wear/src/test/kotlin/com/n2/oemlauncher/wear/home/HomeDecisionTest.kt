package com.n2.oemlauncher.wear.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDecisionTest {
    @Test fun watchFaceShowsLauncher() = assertEquals(
        HomeAction.SHOW_LAUNCHER,
        decideHomeAction(ForegroundKind.WATCH_FACE, launcherResumed = false, LastHomeAction.NONE),
    )

    @Test fun launcherOrAppDelegates() {
        assertEquals(HomeAction.DELEGATE_TO_SYSTEM, decideHomeAction(ForegroundKind.LAUNCHER, false, LastHomeAction.NONE))
        assertEquals(HomeAction.DELEGATE_TO_SYSTEM, decideHomeAction(ForegroundKind.APP, false, LastHomeAction.NONE))
    }

    @Test fun unknownResumedDelegates() = assertEquals(
        HomeAction.DELEGATE_TO_SYSTEM,
        decideHomeAction(ForegroundKind.UNKNOWN, launcherResumed = true, LastHomeAction.DELEGATED_TO_SYSTEM),
    )

    @Test fun unknownAfterDelegationShowsLauncher() = assertEquals(
        HomeAction.SHOW_LAUNCHER,
        decideHomeAction(ForegroundKind.UNKNOWN, launcherResumed = false, LastHomeAction.DELEGATED_TO_SYSTEM),
    )

    @Test fun unknownOtherwiseDelegates() = assertEquals(
        HomeAction.DELEGATE_TO_SYSTEM,
        decideHomeAction(ForegroundKind.UNKNOWN, launcherResumed = false, LastHomeAction.OPENED_APP),
    )
}
