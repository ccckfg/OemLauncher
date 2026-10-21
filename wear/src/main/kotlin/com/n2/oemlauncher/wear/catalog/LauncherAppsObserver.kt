package com.n2.oemlauncher.wear.catalog

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import com.n2.oemlauncher.wear.config.WearDebugLog

class LauncherAppsObserver(
    context: Context,
    private val onChanged: () -> Unit,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: android.os.UserHandle) = onChanged()
        override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) = onChanged()
        override fun onPackageChanged(packageName: String, user: android.os.UserHandle) = onChanged()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = onChanged()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = onChanged()
    }

    fun start(): Boolean = runCatching {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        true
    }.onFailure { error ->
        WearDebugLog.d { "catalog observer start registered=false error=${error.javaClass.simpleName}" }
    }.getOrDefault(false)

    fun stop() {
        runCatching { launcherApps.unregisterCallback(callback) }
            .onFailure { error -> WearDebugLog.d { "catalog observer stop error=${error.javaClass.simpleName}" } }
    }
}
