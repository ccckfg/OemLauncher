package com.n2.oemlauncher.shared.layout

import kotlin.math.abs
import kotlin.math.sqrt

/** Geometry helpers for laying out rows on a round screen. */
object RoundScreen {

    /** Half the chord width of a circle of [radius] at vertical [offset] from its center. */
    fun chordHalfWidth(offset: Float, radius: Float): Float {
        val y = abs(offset)
        return if (y >= radius) 0f else sqrt(radius * radius - y * y)
    }

    /** Rows needed to show [itemCount] items in [columns] columns. */
    fun rowCount(itemCount: Int, columns: Int): Int =
        if (itemCount <= 0 || columns <= 0) 0 else (itemCount + columns - 1) / columns
}
