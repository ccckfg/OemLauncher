package com.n2.oemlauncher.wear.home

import android.app.Activity
import android.app.ActivityManager

/**
 * Samsung's China firmware pushes its app-list permission prompt into the launcher's own task.
 * Removing the task on stop would close that prompt with it, and the emptied home task makes the
 * system start Home again, which lands on the stock watch face.
 */
fun Activity.isCoveredInOwnTask(): Boolean {
    val tasks = getSystemService(ActivityManager::class.java)?.appTasks.orEmpty()
    val top = tasks.firstNotNullOfOrNull { task ->
        runCatching { task.taskInfo }.getOrNull()?.takeIf { it.taskId == taskId }
    }?.topActivity ?: return false
    return top.className != javaClass.name
}
