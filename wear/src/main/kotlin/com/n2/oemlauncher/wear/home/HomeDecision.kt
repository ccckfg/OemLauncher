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
    ForegroundKind.WATCH_FACE -> HomeAction.SHOW_LAUNCHER
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
    private var kind: ForegroundKind = ForegroundKind.UNKNOWN

    @Volatile
    private var launcherResumed: Boolean = false

    @Volatile
    private var lastAction: LastHomeAction = LastHomeAction.NONE

    fun updateForeground(packageName: String, inputMethodPackage: String? = null) {
        if (packageName.isBlank() || packageName == "android" || packageName == "com.android.systemui" ||
            packageName == inputMethodPackage
        ) return
        kind = when {
            packageName == systemHomePackage -> ForegroundKind.WATCH_FACE
            packageName == launcherPackage -> ForegroundKind.LAUNCHER
            else -> ForegroundKind.APP
        }
    }

    fun markServiceConnectedOnSystemHome() {
        if (!launcherResumed && kind == ForegroundKind.UNKNOWN) kind = ForegroundKind.WATCH_FACE
    }

    fun setLauncherResumed(resumed: Boolean) {
        launcherResumed = resumed
        if (resumed) kind = ForegroundKind.LAUNCHER
    }

    fun recordHomeAction(action: HomeAction) {
        if (action == HomeAction.DELEGATE_TO_SYSTEM) lastAction = LastHomeAction.DELEGATED_TO_SYSTEM
    }

    fun recordAppLaunch() {
        lastAction = LastHomeAction.OPENED_APP
        kind = ForegroundKind.APP
    }

    fun snapshot(): ForegroundSnapshot = ForegroundSnapshot(kind, launcherResumed, lastAction)
}
