package com.n2.oemlauncher.wear.tile

import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.ordering.AppOrdering
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.wear.config.WearConstants

/** One favorite shown in the tile. */
data class TileFavorite(
    val key: String,
    val label: String,
    /** Custom icon when the config overrides it, otherwise the catalog's default icon. */
    val iconId: String?,
)

/**
 * Config + catalog snapshot -> tile entries. Pure on purpose: the launcher ordering rule is the
 * shared one, so the tile shows exactly the favorites the honeycomb center shows.
 */
object FavoritesTileModel {

    /** Favorites in launcher order, capped for the tile; empty when none are visible. */
    fun favorites(
        config: LauncherConfig,
        catalog: WatchCatalog?,
        max: Int = WearConstants.TILE_FAVORITES_MAX,
    ): List<TileFavorite> =
        AppOrdering.arrange(catalog?.entries.orEmpty(), config).favorites
            .take(max)
            .map { entry -> TileFavorite(entry.key, entry.label, config.iconOverrides[entry.key] ?: entry.iconId) }

    /**
     * Version of the rendered tile resources: changes exactly when the favorites, their icons,
     * or the availability of those icons changes, so the host re-requests what it must.
     */
    fun resourcesVersion(favorites: List<TileFavorite>, availableIconIds: Set<String>): String {
        val text = favorites.joinToString("\u0000") { favorite ->
            val icon = favorite.iconId.orEmpty()
            "${favorite.key}\u0001$icon\u0001${favorite.iconId != null && favorite.iconId in availableIconIds}"
        }
        return IconId.of(text.encodeToByteArray())
    }
}
