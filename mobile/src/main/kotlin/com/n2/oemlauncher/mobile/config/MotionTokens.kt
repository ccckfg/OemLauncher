package com.n2.oemlauncher.mobile.config

/** Page transitions, overlays and draggable controls (design section 7, motion). */
object MotionTokens {
    /** A pushed page slides in over this share of the width while it fades up. */
    const val PAGE_SHIFT = 0.28f

    /** The page underneath drifts the other way by this share: parallax, not a full slide. */
    const val PAGE_PARALLAX = 0.08f
    const val PAGE_SCALE = 0.97f
    const val PAGE_MS = 420
    const val PAGE_FADE_IN_MS = 300
    const val PAGE_FADE_OUT_MS = 180

    /** Material 3 "emphasized decelerate": fast off the mark, long soft landing. */
    const val EASE_X1 = 0.05f
    const val EASE_Y1 = 0.7f
    const val EASE_X2 = 0.1f
    const val EASE_Y2 = 1f

    const val TITLE_SHIFT_DP = 24

    const val SHEET_DISMISS_FRACTION = 0.3f
    const val SHEET_DISMISS_VELOCITY = 1200f
    const val SHEET_DAMPING = 0.82f
    const val SHEET_STIFFNESS = 420f
    const val SHEET_EXIT_MS = 260
    const val DIALOG_SCALE = 0.9f
}
