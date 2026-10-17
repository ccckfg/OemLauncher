package com.n2.oemlauncher.wear.home

import android.app.ActivityManager
import android.content.Context
import com.n2.oemlauncher.wear.MainActivity

/** Removes stale launcher tasks before the system home task becomes visible. */
fun Context.removeOtherLauncherTasks(currentTaskId: Int) {
    getSystemService(ActivityManager::class.java)?.appTasks
        ?.filter { task ->
            task.taskInfo?.id != currentTaskId &&
                task.taskInfo?.baseIntent?.component?.packageName == MainActivity::class.java.name.substringBeforeLast('.')
        }
        ?.forEach { task -> task.finishAndRemoveTask() }
}
