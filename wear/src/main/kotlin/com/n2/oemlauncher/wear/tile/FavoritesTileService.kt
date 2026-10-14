package com.n2.oemlauncher.wear.tile

import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture

class FavoritesTileService : TileService() {
    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val root = Column.Builder().addContent(Text.Builder().setText("N2 Favorites").build()).build()
        val layout = Layout.Builder().setRoot(root).build()
        val timeline = Timeline.Builder().addTimelineEntry(TimelineEntry.Builder().setLayout(layout).build()).build()
        val tile = Tile.Builder().setResourcesVersion("1").setFreshnessIntervalMillis(60_000L).setTimeline(timeline).build()
        return CallbackToFutureAdapter.getFuture { completer -> completer.set(tile); null }
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        CallbackToFutureAdapter.getFuture { completer -> completer.set(Resources.Builder().setVersion("1").build()); null }
}
