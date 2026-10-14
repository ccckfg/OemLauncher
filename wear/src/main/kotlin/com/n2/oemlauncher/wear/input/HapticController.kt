package com.n2.oemlauncher.wear.input

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class HapticController(context: Context) {
    private val vibrator = context.getSystemService(Vibrator::class.java)

    fun tick(enabled: Boolean) {
        if (!enabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        else @Suppress("DEPRECATION") vibrator.vibrate(12L)
    }
}
