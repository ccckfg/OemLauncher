package com.n2.oemlauncher.wear.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.n2.oemlauncher.wear.App
import com.n2.oemlauncher.wear.MainActivity
import com.n2.oemlauncher.wear.config.WearConstants
import com.n2.oemlauncher.wear.config.WearDebugLog

/** Transparent HOME entry point; it decides before the launcher surface is drawn. */
@SuppressLint("WearRecents")
class HomeDispatchActivity : Activity() {
    private val container get() = (application as App).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatchHome(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        dispatchHome(intent ?: this.intent)
    }

    private fun dispatchHome(homeIntent: Intent) {
        WearDebugLog.d {
            "dispatch intent action=${homeIntent.action ?: "-"} " +
                "categories=${homeIntent.categories?.sorted()?.joinToString("|") ?: "-"} " +
                "flags=0x${homeIntent.flags.toString(16)} " +
                "extras=${homeIntent.extras?.keySet()?.sorted()?.joinToString("|") ?: "-"}"
        }
        val snapshot = container.foregroundState.snapshot()
        val fromHomeKey = homeIntent.getBooleanExtra(WearConstants.EXTRA_FROM_HOME_KEY, false)
        val action = decideHomeAction(snapshot.kind, snapshot.launcherResumed, snapshot.lastAction, fromHomeKey)
        WearDebugLog.d { "dispatch snapshot=$snapshot fromHomeKey=$fromHomeKey decision=$action" }
        val systemTarget = if (action == HomeAction.DELEGATE_TO_SYSTEM) {
            container.systemHomeResolver.resolveIntent(homeIntent)
        } else null
        val resolvedHomePackage = systemTarget?.component?.packageName
        if (resolvedHomePackage != null) {
            container.foregroundState.systemHomePackage = resolvedHomePackage
        } else if (action == HomeAction.DELEGATE_TO_SYSTEM) {
            container.refreshSystemHomePackage()
        }
        WearDebugLog.d {
            "dispatch systemHome=${systemTarget?.component?.flattenToShortString() ?: "null"}"
        }
        val actualAction = if (systemTarget == null) HomeAction.SHOW_LAUNCHER else action
        val target = systemTarget ?: Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        WearDebugLog.d {
            "dispatch start target=${target.component?.flattenToShortString() ?: target.action ?: "-"} " +
                "action=$actualAction"
        }
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
