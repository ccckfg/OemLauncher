package com.n2.oemlauncher.mobile.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.n2.oemlauncher.mobile.config.MotionTokens

/**
 * Push and pop between pages: the new page slides in a short way and fades up while the one
 * underneath drifts back and dims, so the move reads as depth rather than a hard cut.
 */
internal object PageTransitions {
    private val emphasized = CubicBezierEasing(
        MotionTokens.EASE_X1, MotionTokens.EASE_Y1, MotionTokens.EASE_X2, MotionTokens.EASE_Y2,
    )
    private fun <T> slide() = tween<T>(MotionTokens.PAGE_MS, easing = emphasized)
    private fun shift(width: Int) = (width * MotionTokens.PAGE_SHIFT).toInt()
    private fun parallax(width: Int) = (width * MotionTokens.PAGE_PARALLAX).toInt()

    fun enter(reduced: Boolean): EnterTransition = if (reduced) EnterTransition.None else {
        slideInHorizontally(slide()) { shift(it) } +
            fadeIn(tween(MotionTokens.PAGE_FADE_IN_MS)) +
            scaleIn(slide(), initialScale = MotionTokens.PAGE_SCALE)
    }

    fun exit(reduced: Boolean): ExitTransition = if (reduced) ExitTransition.None else {
        slideOutHorizontally(slide()) { -parallax(it) } + fadeOut(tween(MotionTokens.PAGE_FADE_OUT_MS))
    }

    fun popEnter(reduced: Boolean): EnterTransition = if (reduced) EnterTransition.None else {
        slideInHorizontally(slide()) { -parallax(it) } + fadeIn(tween(MotionTokens.PAGE_FADE_IN_MS))
    }

    fun popExit(reduced: Boolean): ExitTransition = if (reduced) ExitTransition.None else {
        slideOutHorizontally(slide()) { shift(it) } +
            fadeOut(tween(MotionTokens.PAGE_FADE_OUT_MS)) +
            scaleOut(slide(), targetScale = MotionTokens.PAGE_SCALE)
    }
}
