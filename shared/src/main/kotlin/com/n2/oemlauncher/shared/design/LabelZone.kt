package com.n2.oemlauncher.shared.design

/**
 * The honeycomb's curved app name sits on the bottom edge of the round screen with no band
 * behind it. Cells that scroll into that strip fade out so the name stays readable, and the name
 * itself gets an outline in the background color. Watch and phone preview must use the same
 * numbers, otherwise the preview stops matching the watch.
 */
object LabelZone {
    /** Depth of the strip, as a fraction of the screen radius, measured up from the bottom edge. */
    const val DEPTH_FRACTION = 0.2f

    /** Opacity of a cell that lies completely inside the strip. */
    const val MIN_ALPHA = 0.3f

    /** Outline drawn under the name in the palette's background color. */
    const val OUTLINE_WIDTH_DP = 3f
    const val OUTLINE_ALPHA = 0.9f

    /**
     * Opacity multiplier for one cell. It ramps smoothly with how much of the cell has entered the
     * strip, so cells never pop while the honeycomb scrolls.
     *
     * @param cellCenterY distance from the screen center to the cell center, positive downwards
     * @param cellRadius the cell's radius after edge scaling
     * @param screenRadius radius of the round screen
     */
    fun alpha(cellCenterY: Float, cellRadius: Float, screenRadius: Float): Float {
        if (cellRadius <= 0f) return 1f
        val stripTop = screenRadius * (1f - DEPTH_FRACTION)
        val inside = ((cellCenterY + cellRadius - stripTop) / (2f * cellRadius)).coerceIn(0f, 1f)
        return 1f - (1f - MIN_ALPHA) * inside
    }
}
