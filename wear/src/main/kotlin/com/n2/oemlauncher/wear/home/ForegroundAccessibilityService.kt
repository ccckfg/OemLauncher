package com.n2.oemlauncher.wear.home

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.app.WallpaperManager
import android.view.accessibility.AccessibilityEvent
import android.provider.Settings
import com.n2.oemlauncher.wear.App
import com.n2.oemlauncher.wear.config.WearDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ForegroundAccessibilityService : AccessibilityService() {
    private val holder get() = (application as App).container.foregroundState
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        holder.setServiceConnected(true)
        holder.markServiceConnectedOnSystemHome()
        WearDebugLog.d { "a11y connected" }
        val app = application as App
        serviceScope.launch {
            val systemPackage = app.container.systemHomeResolver.packageName()
            val wallpaperPackage = runCatching {
                WallpaperManager.getInstance(this@ForegroundAccessibilityService).wallpaperInfo?.packageName
            }.getOrNull()
            holder.setHomePackages(systemPackage, wallpaperPackage)
            WearDebugLog.d {
                "a11y homePackages system=${systemPackage ?: "null"} wallpaper=${wallpaperPackage ?: "null"}"
            }
        }
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = 0
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        holder.setServiceConnected(false)
        WearDebugLog.d { "a11y disconnected reason=unbind" }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        holder.setServiceConnected(false)
        serviceScope.cancel()
        WearDebugLog.d { "a11y disconnected reason=destroy" }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        val className = event?.className?.toString().orEmpty()
        if (className == HomeDispatchActivity::class.java.name) {
            WearDebugLog.d { "a11y event package=${packageName.ifBlank { "-" }} class=$className kind=DISPATCH" }
            return
        }
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
        val kind = holder.updateForeground(packageName, ime)
        WearDebugLog.d {
            "a11y event package=${packageName.ifBlank { "-" }} class=${className.ifBlank { "-" }} " +
                "kind=${kind?.name ?: "IGNORED"}"
        }
    }

    override fun onInterrupt() = Unit
}
