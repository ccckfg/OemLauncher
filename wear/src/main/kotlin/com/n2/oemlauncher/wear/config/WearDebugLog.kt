package com.n2.oemlauncher.wear.config

import android.util.Log
import com.n2.oemlauncher.wear.BuildConfig

/** Debug-only diagnostics for the Home dispatch path; release builds do not evaluate messages. */
object WearDebugLog {
    inline fun d(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(WearConstants.DEBUG_LOG_TAG, message())
    }
}
