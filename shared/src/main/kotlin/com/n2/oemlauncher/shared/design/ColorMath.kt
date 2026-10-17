package com.n2.oemlauncher.shared.design

import kotlin.math.pow

/** WCAG 2.x contrast helpers over 0xAARRGGBB colors, used to keep the palettes legible. */
object ColorMath {
    fun alpha(argb: Long): Float = ((argb shr 24) and 0xFF) / 255f

    /** Flattens a translucent [top] over an opaque [bottom], as the screen would show it. */
    fun compositeOver(top: Long, bottom: Long): Long {
        val a = alpha(top)
        fun channel(shift: Int): Long {
            val t = (top shr shift) and 0xFF
            val b = (bottom shr shift) and 0xFF
            return (t * a + b * (1 - a) + 0.5f).toLong().coerceIn(0, 255)
        }
        return (0xFFL shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun relativeLuminance(argb: Long): Double {
        fun linear(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(16) + 0.7152 * linear(8) + 0.0722 * linear(0)
    }

    /** Contrast ratio between two opaque colors, from 1.0 to 21.0. */
    fun contrast(a: Long, b: Long): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
