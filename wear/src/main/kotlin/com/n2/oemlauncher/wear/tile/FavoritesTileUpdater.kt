package com.n2.oemlauncher.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService

/** Asks the tile host to re-request this tile; called whenever the applied config changes. */
object FavoritesTileUpdater {
    fun request(context: Context) {
        try {
            TileService.getUpdater(context).requestUpdate(FavoritesTileService::class.java)
        } catch (_: RuntimeException) {
            // No tile host (for example without Play services): nothing to refresh.
        }
    }
}
