package com.n2.oemlauncher.shared.config

import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconSize
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.model.VisualTier

/** Single source of defaults and allowed ranges for LauncherConfig and LayoutSettings. */
object LauncherDefaults {
    val LAYOUT_MODE = LayoutMode.HONEYCOMB
    val ICON_SIZE = IconSize.MEDIUM
    val ICON_SHAPE = IconShape.CIRCLE
    val SORT_MODE = SortMode.CUSTOM

    const val FISHEYE_MIN = 0f
    const val FISHEYE_MAX = 1f
    const val FISHEYE_DEFAULT = 0.7f

    const val GRID_COLUMNS_MIN = 2
    const val GRID_COLUMNS_MAX = 4
    const val GRID_COLUMNS_DEFAULT = 3

    const val SHOW_LABELS = true
    const val SNAP_TO_CENTER = true
    const val HAPTICS_ENABLED = true
    const val REDUCE_MOTION = false
    val VISUAL_TIER = VisualTier.REFINED
    val ICON_STYLE = IconStyle.ORIGINAL

    /** Center cell plus the first ring of the honeycomb. */
    const val MAX_FAVORITES = 7
    const val MAX_ORDER_ENTRIES = 600
    const val MAX_HIDDEN = 600
    const val MAX_ICON_OVERRIDES = 300
    const val MAX_LABEL_LENGTH = 80
}
