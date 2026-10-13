package com.n2.oemlauncher.shared.config

import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.protocol.IconId

/**
 * Normalizes configs from storage or from the other device: canonical keys, no duplicates,
 * size caps and numeric ranges. Every decode path runs through here, so the rest of the code
 * can trust a [LauncherConfig] it receives.
 */
object ConfigSanitizer {

    fun sanitize(config: LauncherConfig): LauncherConfig {
        val hidden = canonicalKeys(config.hidden, LauncherDefaults.MAX_HIDDEN)
        val hiddenSet = hidden.toHashSet()
        val favorites = canonicalKeys(config.favorites, LauncherDefaults.MAX_FAVORITES)
            .filterNot { it in hiddenSet }
        return config.copy(
            revision = config.revision.coerceAtLeast(0),
            editedAtMillis = config.editedAtMillis.coerceAtLeast(0),
            layout = sanitizeLayout(config.layout),
            order = canonicalKeys(config.order, LauncherDefaults.MAX_ORDER_ENTRIES),
            favorites = favorites,
            hidden = hidden,
            iconOverrides = sanitizeOverrides(config.iconOverrides),
        )
    }

    fun sanitizeLayout(layout: LayoutSettings): LayoutSettings {
        val strength = layout.fisheyeStrength
        val safeStrength = if (strength.isNaN()) {
            LauncherDefaults.FISHEYE_DEFAULT
        } else {
            strength.coerceIn(LauncherDefaults.FISHEYE_MIN, LauncherDefaults.FISHEYE_MAX)
        }
        return layout.copy(
            fisheyeStrength = safeStrength,
            gridColumns = layout.gridColumns.coerceIn(
                LauncherDefaults.GRID_COLUMNS_MIN,
                LauncherDefaults.GRID_COLUMNS_MAX,
            ),
        )
    }

    private fun canonicalKeys(keys: List<String>, limit: Int): List<String> {
        val result = LinkedHashSet<String>()
        for (key in keys) {
            if (result.size >= limit) break
            AppKey.canonical(key)?.let(result::add)
        }
        return result.toList()
    }

    private fun sanitizeOverrides(overrides: Map<String, String>): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for ((key, iconId) in overrides) {
            if (result.size >= LauncherDefaults.MAX_ICON_OVERRIDES) break
            val canonicalKey = AppKey.canonical(key) ?: continue
            if (IconId.isValid(iconId)) result[canonicalKey] = iconId
        }
        return result
    }
}
