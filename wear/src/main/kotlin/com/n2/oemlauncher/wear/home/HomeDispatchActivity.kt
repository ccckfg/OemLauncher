package com.n2.oemlauncher.wear.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.n2.oemlauncher.wear.App
import com.n2.oemlauncher.wear.MainActivity

/** Transparent HOME entry point; it decides before the launcher surface is drawn. */
@SuppressLint("WearRecents")
class HomeDispatchActivity : Activity() {
    private val container get() = (application as App).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatchHome()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        dispatchHome()
    }

    private fun dispatchHome() {
        val snapshot = container.foregroundState.snapshot()
        val action = decideHomeAction(snapshot.kind, snapshot.launcherResumed, snapshot.lastAction)
        val systemTarget = if (action == HomeAction.DELEGATE_TO_SYSTEM) container.systemHomeResolver.resolveIntent() else null
        val actualAction = if (systemTarget == null) HomeAction.SHOW_LAUNCHER else action
        val target = systemTarget ?: Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        removeOtherLauncherTasks(taskId)
        container.foregroundState.recordHomeAction(actualAction)
        startActivity(target)
        overridePendingTransition(0, 0)
        if (actualAction == HomeAction.DELEGATE_TO_SYSTEM) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }
}
