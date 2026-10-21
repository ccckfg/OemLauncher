package com.n2.oemlauncher.shared.model

import com.n2.oemlauncher.shared.config.LauncherDefaults
import kotlinx.serialization.Serializable

/** Visual settings of the watch launcher. Ranges are enforced by ConfigSanitizer. */
@Serializable
data class LayoutSettings(
    val mode: LayoutMode = LauncherDefaults.LAYOUT_MODE,
    val iconSize: IconSize = LauncherDefaults.ICON_SIZE,
    val iconShape: IconShape = LauncherDefaults.ICON_SHAPE,
    /** 0 = flat honeycomb, 1 = strongest edge shrink. */
    val fisheyeStrength: Float = LauncherDefaults.FISHEYE_DEFAULT,
    val gridColumns: Int = LauncherDefaults.GRID_COLUMNS_DEFAULT,
    /** Labels in grid and list modes; the honeycomb always labels only the focused app. */
    val showLabels: Boolean = LauncherDefaults.SHOW_LABELS,
    /** After a fling, settle so the nearest app sits exactly in the center. */
    val snapToCenter: Boolean = LauncherDefaults.SNAP_TO_CENTER,
    val hapticsEnabled: Boolean = LauncherDefaults.HAPTICS_ENABLED,
    /** Replace motion (fling physics, scale animations) with instant transitions. */
    val reduceMotion: Boolean = LauncherDefaults.REDUCE_MOTION,
    /** Added in v2; older peers ignore it and decode missing values as the default. */
    val visualTier: VisualTier = LauncherDefaults.VISUAL_TIER,
    /** Added in v2; older peers ignore it and decode missing values as the default. */
    val iconStyle: IconStyle = LauncherDefaults.ICON_STYLE,
)
