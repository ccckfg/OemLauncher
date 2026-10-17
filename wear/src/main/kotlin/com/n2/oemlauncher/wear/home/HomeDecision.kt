package com.n2.oemlauncher.wear.home

/** The foreground categories used by the Home-key policy. */
enum class ForegroundKind { UNKNOWN, WATCH_FACE, LAUNCHER, APP }

enum class LastHomeAction { NONE, DELEGATED_TO_SYSTEM, OPENED_APP }

enum class HomeAction { SHOW_LAUNCHER, DELEGATE_TO_SYSTEM }

/**
 * Pure Home-key policy. The accessibility service normally supplies [foreground], while the
 * remaining inputs make the behavior deterministic when the service is disabled or delayed.
 */
fun decideHomeAction(
    foreground: ForegroundKind,
    launcherResumed: Boolean,
    lastAction: LastHomeAction,
): HomeAction = when (foreground) {
    // During the Home transition the accessibility service can report SystemUI before the
    // launcher has received onStop; the lifecycle signal keeps that race on the system home.
    ForegroundKind.WATCH_FACE -> if (launcherResumed) HomeAction.DELEGATE_TO_SYSTEM else HomeAction.SHOW_LAUNCHER
    ForegroundKind.LAUNCHER, ForegroundKind.APP -> HomeAction.DELEGATE_TO_SYSTEM
    ForegroundKind.UNKNOWN -> when {
        launcherResumed -> HomeAction.DELEGATE_TO_SYSTEM
        lastAction == LastHomeAction.DELEGATED_TO_SYSTEM -> HomeAction.SHOW_LAUNCHER
        else -> HomeAction.DELEGATE_TO_SYSTEM
    }
}

data class ForegroundSnapshot(
    val kind: ForegroundKind,
    val launcherResumed: Boolean,
    val lastAction: LastHomeAction,
)

/** Process-local state shared by the Activity, Home trampoline and accessibility service. */
class ForegroundStateHolder(private val launcherPackage: String) {
    @Volatile
    var systemHomePackage: String? = null

    @Volatile
    var watchFacePackage: String? = null

    @Volatile
    private var kind: ForegroundKind = ForegroundKind.UNKNOWN

    @Volatile
    private var launcherResumed: Boolean = false

    @Volatile
    private var lastAction: LastHomeAction = LastHomeAction.NONE

    @Volatile
    private var serviceConnected: Boolean = false

    fun updateForeground(packageName: String, inputMethodPackage: String? = null): ForegroundKind? {
        if (packageName.isBlank() || packageName == "android" || packageName == "com.android.systemui" ||
            packageName == inputMethodPackage
        ) return null
        kind = when {
            packageName == systemHomePackage || packageName == watchFacePackage -> ForegroundKind.WATCH_FACE
            packageName == launcherPackage -> ForegroundKind.LAUNCHER
            else -> ForegroundKind.APP
        }
        return kind
    }

    fun markServiceConnectedOnSystemHome() {
        if (!launcherResumed && kind == ForegroundKind.UNKNOWN) kind = ForegroundKind.WATCH_FACE
    }

    fun setServiceConnected(connected: Boolean) {
        serviceConnected = connected
        if (!connected) kind = ForegroundKind.UNKNOWN
    }

    fun setHomePackages(systemPackage: String?, wallpaperPackage: String?) {
        systemHomePackage = systemPackage
        watchFacePackage = wallpaperPackage
        markServiceConnectedOnSystemHome()
    }

    fun setLauncherResumed(resumed: Boolean) {
        launcherResumed = resumed
        if (resumed) kind = ForegroundKind.LAUNCHER
    }

    fun recordHomeAction(action: HomeAction) {
        if (action == HomeAction.DELEGATE_TO_SYSTEM) {
            lastAction = LastHomeAction.DELEGATED_TO_SYSTEM
            kind = ForegroundKind.WATCH_FACE
        }
    }

    fun recordAppLaunch() {
        lastAction = LastHomeAction.OPENED_APP
        kind = ForegroundKind.APP
    }

    fun snapshot(): ForegroundSnapshot = ForegroundSnapshot(
        kind = if (serviceConnected) kind else ForegroundKind.UNKNOWN,
        launcherResumed = launcherResumed,
        lastAction = lastAction,
    )
}
