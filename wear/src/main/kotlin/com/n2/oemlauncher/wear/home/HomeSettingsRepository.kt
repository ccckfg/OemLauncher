package com.n2.oemlauncher.wear.home

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings

data class HomeSettingsState(
    val isDefaultHome: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val message: String? = null,
)

class HomeSettingsRepository(private val context: Context) {
    fun read(): HomeSettingsState = HomeSettingsState(
        isDefaultHome = isDefaultHome(),
        accessibilityEnabled = isAccessibilityEnabled(),
    )

    private fun isDefaultHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addCategory(Intent.CATEGORY_DEFAULT)
        return context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName == context.packageName
    }

    private fun isAccessibilityEnabled(): Boolean {
        val manager = context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
        return manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { info ->
                info.resolveInfo?.serviceInfo?.packageName == context.packageName &&
                    info.resolveInfo?.serviceInfo?.name == ForegroundAccessibilityService::class.java.name
            } == true
    }

    companion object {
        fun homeSettingsIntents(): List<Intent> = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        )
    }
}
