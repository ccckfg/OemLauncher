package com.n2.oemlauncher.wear.catalog

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager

const val SAMSUNG_APP_LIST_PERMISSION = "com.samsung.android.permission.GET_APP_LIST"

data class AppListPermissionStatus(
    val available: Boolean,
    val granted: Boolean,
    val shouldShowRationale: Boolean,
    val requestAttempted: Boolean,
)

enum class EmptyCatalogState { GENERIC, NEEDS_PERMISSION, PERMISSION_BLOCKED }

/** Pure mapping so non-Samsung devices can use the ordinary empty state. */
fun emptyCatalogState(status: AppListPermissionStatus): EmptyCatalogState = when {
    !status.available -> EmptyCatalogState.GENERIC
    status.granted -> EmptyCatalogState.GENERIC
    status.requestAttempted && !status.shouldShowRationale -> EmptyCatalogState.PERMISSION_BLOCKED
    else -> EmptyCatalogState.NEEDS_PERMISSION
}

class AppListPermissionController(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun isAvailable(): Boolean = runCatching {
        packageManager.getPermissionInfo(SAMSUNG_APP_LIST_PERMISSION, 0)
        true
    }.getOrDefault(false)

    fun status(requestAttempted: Boolean, shouldShowRationale: Boolean): AppListPermissionStatus {
        val available = isAvailable()
        val granted = available && appContext.checkSelfPermission(SAMSUNG_APP_LIST_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        return AppListPermissionStatus(available, granted, shouldShowRationale, requestAttempted)
    }

    fun request(activity: Activity) {
        if (isAvailable()) activity.requestPermissions(arrayOf(SAMSUNG_APP_LIST_PERMISSION), REQUEST_CODE)
    }

    companion object {
        const val REQUEST_CODE = 4107
    }
}
