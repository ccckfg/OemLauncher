package com.n2.oemlauncher.wear.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDecisionTest {
    @Test fun watchFaceShowsLauncher() = assertEquals(
        HomeAction.SHOW_LAUNCHER,
        decideHomeAction(ForegroundKind.WATCH_FACE, launcherResumed = false, LastHomeAction.NONE),
    )

    @Test fun watchFaceDuringLauncherTransitionDelegates() = assertEquals(
        HomeAction.DELEGATE_TO_SYSTEM,
        decideHomeAction(ForegroundKind.WATCH_FACE, launcherResumed = true, LastHomeAction.NONE),
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

    @Test fun homeSentByTheSystemNeverOpensTheLauncher() {
        for (kind in ForegroundKind.entries) for (last in LastHomeAction.entries) for (resumed in listOf(true, false)) {
            assertEquals(
                "$kind $last resumed=$resumed",
                HomeAction.DELEGATE_TO_SYSTEM,
                decideHomeAction(kind, resumed, last, fromHomeKey = false),
            )
        }
    }

    @Test fun screenOffRehomeLoopCannotStart() {
        // Screen off with the service disconnected: the launcher stops, its task goes away and the
        // system sends HOME without the key extra. That HOME must land on the watch face.
        val holder = ForegroundStateHolder("com.n2.oemlauncher")
        holder.recordHomeAction(HomeAction.DELEGATE_TO_SYSTEM)
        holder.setLauncherResumed(true)
        holder.setLauncherResumed(false)
        val snapshot = holder.snapshot()
        assertEquals(
            HomeAction.DELEGATE_TO_SYSTEM,
            decideHomeAction(snapshot.kind, snapshot.launcherResumed, snapshot.lastAction, fromHomeKey = false),
        )
    }

    @Test fun unknownOtherwiseDelegates() = assertEquals(
        HomeAction.DELEGATE_TO_SYSTEM,
        decideHomeAction(ForegroundKind.UNKNOWN, launcherResumed = false, LastHomeAction.OPENED_APP),
    )

    @Test fun disconnectedServiceUsesHeuristicAfterReturningFromWatchFace() {
        val holder = ForegroundStateHolder("com.n2.oemlauncher")
        holder.setLauncherResumed(false)
        holder.recordHomeAction(HomeAction.DELEGATE_TO_SYSTEM)
        assertEquals(HomeAction.SHOW_LAUNCHER, decide(holder))
        holder.setLauncherResumed(true)
        holder.recordHomeAction(HomeAction.DELEGATE_TO_SYSTEM)
        holder.setLauncherResumed(false)
        assertEquals(HomeAction.SHOW_LAUNCHER, decide(holder))
    }

    @Test fun disconnectingServiceClearsStaleForegroundKind() {
        val holder = ForegroundStateHolder("com.n2.oemlauncher")
        holder.setServiceConnected(true)
        holder.updateForeground("com.n2.oemlauncher")
        holder.setServiceConnected(false)
        assertEquals(ForegroundKind.UNKNOWN, holder.snapshot().kind)
    }

    @Test fun delegatedSystemHomeResetsStuckAppToWatchFace() {
        val holder = ForegroundStateHolder("com.n2.oemlauncher")
        holder.setServiceConnected(true)
        holder.updateForeground("com.example.app")
        assertEquals(ForegroundKind.APP, holder.snapshot().kind)
        holder.recordHomeAction(HomeAction.DELEGATE_TO_SYSTEM)
        assertEquals(ForegroundKind.WATCH_FACE, holder.snapshot().kind)
        assertEquals(HomeAction.SHOW_LAUNCHER, decide(holder))
    }

    @Test fun wallpaperPackageIsClassifiedAsWatchFace() {
        val holder = ForegroundStateHolder("com.n2.oemlauncher")
        holder.setServiceConnected(true)
        holder.setHomePackages(null, "com.example.watchface")
        holder.updateForeground("com.example.watchface")
        assertEquals(ForegroundKind.WATCH_FACE, holder.snapshot().kind)
    }

    private fun decide(holder: ForegroundStateHolder): HomeAction {
        val snapshot = holder.snapshot()
        return decideHomeAction(snapshot.kind, snapshot.launcherResumed, snapshot.lastAction)
    }
}
