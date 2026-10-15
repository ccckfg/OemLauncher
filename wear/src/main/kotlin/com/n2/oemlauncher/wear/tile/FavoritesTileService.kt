package com.n2.oemlauncher.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.wear.App
import com.n2.oemlauncher.wear.R
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Favorites tile: reads the config and the catalog snapshot, shows the favorites in launcher
 * order with their icons, and launches an app when a row is tapped.
 */
class FavoritesTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> =
        load { content ->
            TileLayoutBuilder.buildTile(
                content,
                header = getString(R.string.tile_header),
                emptyText = getString(R.string.tile_empty),
                openLauncherText = getString(R.string.tile_open_launcher),
                scope = requestParams.scope,
            )
        }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        load { content -> TileLayoutBuilder.buildResources(content, requestParams.version) }

    /** Reads everything off the main thread and completes the tile future when done. */
    private fun <T> load(build: (TileContent) -> T): ListenableFuture<T> =
        CallbackToFutureAdapter.getFuture<T> { completer ->
            scope.launch {
                val result = try {
                    build(content())
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Never crash the tile host over missing data or a render failure.
                    build(fallbackContent())
                }
                completer.set(result)
            }
            null
        }

    private suspend fun content(): TileContent {
        val container = (application as App).container
        val config = container.configRepository.config.first()
        val catalog = container.snapshotStore.read()
        val favorites = FavoritesTileModel.favorites(config, catalog)
        val icons = favorites
            .mapNotNull { favorite -> favorite.iconId?.let { id -> container.iconStore.get(id)?.let { id to it } } }
            .toMap()
        return TileContent(
            favorites = favorites,
            iconBytes = icons,
            resourcesVersion = FavoritesTileModel.resourcesVersion(favorites, icons.keys),
            launcherComponent = launcherComponent(),
        )
    }

    private fun fallbackContent(): TileContent = TileContent(
        favorites = emptyList(),
        iconBytes = emptyMap(),
        resourcesVersion = FavoritesTileModel.resourcesVersion(emptyList(), emptySet()),
        launcherComponent = launcherComponent(),
    )

    private fun launcherComponent(): AppKey? {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val component = intent.component ?: return null
        return AppKey(component.packageName, component.className)
    }
}
