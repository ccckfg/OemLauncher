package com.n2.oemlauncher.shared.layout

/**
 * Honeycomb cell 0 is the settings gear, so the launcher rests with settings in the center and
 * the apps (favorites first) spiral around it. Cells after it map to apps in arranged order.
 * The watch and the phone preview must both use this mapping so they show the same layout.
 */
object HoneycombSlots {
    const val SETTINGS = 0

    fun cellCount(appCount: Int): Int = appCount.coerceAtLeast(0) + 1

    fun isSettings(cell: Int): Boolean = cell == SETTINGS

    /** Index into the arranged apps drawn in [cell], or -1 for the gear and invalid cells. */
    fun appAt(cell: Int): Int = if (cell <= SETTINGS) -1 else cell - 1
}
