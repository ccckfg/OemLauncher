package com.n2.oemlauncher.wear.home

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class SystemHomeResolver(private val context: Context) {
    @SuppressLint("WearRecents")
    fun resolveIntent(sourceIntent: Intent? = null): Intent? {
        val homeIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addCategory(Intent.CATEGORY_DEFAULT)
        val candidates = context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                HomeCandidate(
                    packageName = activity.packageName,
                    className = activity.name,
                    isSystemApp = activity.applicationInfo.flags and
                        (android.content.pm.ApplicationInfo.FLAG_SYSTEM or
                            android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    exported = activity.exported,
                    enabled = activity.enabled && activity.applicationInfo.enabled,
                )
            }
        val selected = selectSystemHome(candidates, context.packageName) ?: return null
        return (sourceIntent?.let(::Intent) ?: homeIntent)
            .setPackage(null)
            .setComponent(ComponentName(selected.packageName, selected.className))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun packageName(): String? = resolveIntent()?.component?.packageName
}
