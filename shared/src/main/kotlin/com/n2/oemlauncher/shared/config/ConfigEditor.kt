package com.n2.oemlauncher.shared.config

import com.n2.oemlauncher.shared.model.ConfigOrigin
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.SortMode

/** Result of [ConfigEditor.toggleFavorite]. */
enum class FavoriteToggle { ADDED, REMOVED, LIMIT_REACHED }

/**
 * Pure editing operations shared by the phone editor and watch-side shortcuts.
 * None of them touch [LauncherConfig.revision]; call [stamp] once when an edit is committed.
 */
object ConfigEditor {

    fun toggleFavorite(config: LauncherConfig, key: String): Pair<LauncherConfig, FavoriteToggle> {
        if (key in config.favorites) {
            return config.copy(favorites = config.favorites - key) to FavoriteToggle.REMOVED
        }
        if (config.favorites.size >= LauncherDefaults.MAX_FAVORITES) {
            return config to FavoriteToggle.LIMIT_REACHED
        }
        val updated = config.copy(favorites = config.favorites + key, hidden = config.hidden - key)
        return updated to FavoriteToggle.ADDED
    }

    fun moveFavorite(config: LauncherConfig, from: Int, to: Int): LauncherConfig =
        config.copy(favorites = config.favorites.moved(from, to))

    /** Hiding an app also removes it from favorites, so both flags never conflict. */
    fun setHidden(config: LauncherConfig, key: String, hidden: Boolean): LauncherConfig =
        if (hidden) {
            config.copy(
                hidden = if (key in config.hidden) config.hidden else config.hidden + key,
                favorites = config.favorites - key,
            )
        } else {
            config.copy(hidden = config.hidden - key)
        }

    /**
     * Moves an app inside [visibleOrder] (the non-favorite apps exactly as currently shown) and
     * stores the result as the custom order. Keys that are not visible now (hidden or
     * uninstalled) keep their remembered relative order after the visible ones.
     */
    fun moveApp(
        config: LauncherConfig,
        visibleOrder: List<String>,
        from: Int,
        to: Int,
    ): LauncherConfig {
        if (from !in visibleOrder.indices || to !in visibleOrder.indices || from == to) {
            return config
        }
        val moved = visibleOrder.moved(from, to)
        val visibleSet = moved.toHashSet()
        val remembered = config.order.filterNot { it in visibleSet }
        return config.copy(order = moved + remembered, sortMode = SortMode.CUSTOM)
    }

    fun setSortMode(config: LauncherConfig, mode: SortMode): LauncherConfig =
        config.copy(sortMode = mode)

    fun setIconOverride(config: LauncherConfig, key: String, iconId: String?): LauncherConfig {
        val overrides = if (iconId == null) {
            config.iconOverrides - key
        } else {
            config.iconOverrides + (key to iconId)
        }
        return config.copy(iconOverrides = overrides)
    }

    fun updateLayout(
        config: LauncherConfig,
        transform: (LayoutSettings) -> LayoutSettings,
    ): LauncherConfig = config.copy(layout = ConfigSanitizer.sanitizeLayout(transform(config.layout)))

    /** Drops references to apps that are no longer installed on the watch. */
    fun pruneTo(config: LauncherConfig, availableKeys: Set<String>): LauncherConfig = config.copy(
        order = config.order.filter { it in availableKeys },
        favorites = config.favorites.filter { it in availableKeys },
        hidden = config.hidden.filter { it in availableKeys },
        iconOverrides = config.iconOverrides.filterKeys { it in availableKeys },
    )

    /** Marks a committed edit; take [revision] from RevisionPolicy.next. */
    fun stamp(
        config: LauncherConfig,
        revision: Long,
        origin: ConfigOrigin,
        nowMillis: Long,
    ): LauncherConfig = config.copy(revision = revision, origin = origin, editedAtMillis = nowMillis)

    private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
        if (from !in indices || to !in indices || from == to) return this
        val result = toMutableList()
        result.add(to, result.removeAt(from))
        return result
    }
}
