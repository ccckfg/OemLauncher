package com.n2.oemlauncher.wear.home

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.provider.Settings
import com.n2.oemlauncher.wear.App

class ForegroundAccessibilityService : AccessibilityService() {
    private val holder get() = (application as App).container.foregroundState

    override fun onServiceConnected() {
        holder.markServiceConnectedOnSystemHome()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = 0
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString().orEmpty()
        if (packageName.isBlank()) return
        if (event?.className?.toString() == HomeDispatchActivity::class.java.name) return
        val ime = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
        holder.updateForeground(packageName, ime)
    }

    override fun onInterrupt() = Unit
}
