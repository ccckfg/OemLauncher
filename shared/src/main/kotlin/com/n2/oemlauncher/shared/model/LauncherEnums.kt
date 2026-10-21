package com.n2.oemlauncher.shared.model

import kotlinx.serialization.Serializable

/** How the watch arranges apps. */
@Serializable
enum class LayoutMode { HONEYCOMB, GRID, LIST }

/** Icon size preset; the dp values live in HoneycombDefaults so both apps render the same. */
@Serializable
enum class IconSize { SMALL, MEDIUM, LARGE }

/** Mask applied to icons on the watch. */
@Serializable
enum class IconShape { CIRCLE, SQUIRCLE, ORIGINAL }

/** Ordering of the non-favorite apps. */
@Serializable
enum class SortMode { CUSTOM, ALPHABETICAL, MOST_USED, RECENTLY_USED }

/** Rendering cost of the watch UI; both tiers share colors and shapes (docs/decisions.md D2). */
@Serializable
enum class VisualTier { REFINED, LITE }

/** ORIGINAL keeps each app's colors; THEMED tints apps that ship a monochrome icon layer. */
@Serializable
enum class IconStyle { ORIGINAL, THEMED }

/** Which device produced a config revision. */
@Serializable
enum class ConfigOrigin { WATCH, PHONE }
