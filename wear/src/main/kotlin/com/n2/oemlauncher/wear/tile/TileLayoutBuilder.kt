package com.n2.oemlauncher.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ProtoLayoutScope
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.TileBuilders
import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.wear.config.WearConstants

/** Everything one tile render needs. */
data class TileContent(
    val favorites: List<TileFavorite>,
    val iconBytes: Map<String, ByteArray>,
    val resourcesVersion: String,
    /** The launcher activity, for the empty-state button; null when it cannot be resolved. */
    val launcherComponent: AppKey?,
)

/**
 * Builds the ProtoLayout tile and its icon resources from [TileContent].
 *
 * Icons are registered as inline resources on the request's [ProtoLayoutScope] (the current
 * image API), so the framework ships them together with the tile; [buildResources] remains the
 * fallback for hosts that request a resource version outside the scope flow.
 */
object TileLayoutBuilder {

    fun buildTile(
        content: TileContent,
        header: String,
        emptyText: String,
        openLauncherText: String,
        scope: ProtoLayoutScope,
    ): TileBuilders.Tile {
        val root = if (content.favorites.isEmpty()) {
            emptyState(content, emptyText, openLauncherText)
        } else {
            favoritesColumn(content, header, scope)
        }
        val layout = LayoutElementBuilders.Layout.Builder().setRoot(root).build()
        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(TimelineBuilders.TimelineEntry.Builder().setLayout(layout).build())
            .build()
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(content.resourcesVersion)
            .setFreshnessIntervalMillis(WearConstants.TILE_FRESHNESS_MILLIS)
            .setTileTimeline(timeline)
            .build()
    }

    fun buildResources(content: TileContent, version: String): ResourceBuilders.Resources {
        val builder = ResourceBuilders.Resources.Builder().setVersion(version)
        content.favorites.forEach { favorite ->
            val iconId = favorite.iconId ?: return@forEach
            val bytes = content.iconBytes[iconId] ?: return@forEach
            builder.addIdToImageMapping(iconId, inlineImageResource(bytes))
        }
        return builder.build()
    }

    /**
     * The icon files are PNG/WebP; the format stays undefined so the renderer detects it from
     * the encoded bytes. Only raw pixel buffers would be declared as a pixel format.
     */
    private fun inlineImageResource(bytes: ByteArray): ResourceBuilders.ImageResource =
        ResourceBuilders.ImageResource.Builder()
            .setInlineResource(
                ResourceBuilders.InlineImageResource.Builder()
                    .setData(bytes)
                    .setWidthPx(WearConstants.TILE_ICON_PX)
                    .setHeightPx(WearConstants.TILE_ICON_PX)
                    .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
                    .build(),
            )
            .build()

    private fun favoritesColumn(
        content: TileContent,
        header: String,
        scope: ProtoLayoutScope,
    ): LayoutElementBuilders.Column {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(safeAreaModifiers())
            .addContent(title(header))
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())
        // 2x2 grid of round icon buttons: every cell stays clear of the round screen edge.
        content.favorites.chunked(TILE_GRID_COLUMNS).forEach { rowFavorites ->
            column.addContent(favoritesGridRow(content, rowFavorites, scope))
            column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
        }
        return column.build()
    }

    private fun favoritesGridRow(
        content: TileContent,
        favorites: List<TileFavorite>,
        scope: ProtoLayoutScope,
    ): LayoutElementBuilders.Row {
        val row = LayoutElementBuilders.Row.Builder()
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        favorites.forEachIndexed { index, favorite ->
            if (index > 0) row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(10f)).build())
            row.addContent(favoriteCell(content, favorite, scope))
        }
        return row.build()
    }

    private fun favoriteCell(
        content: TileContent,
        favorite: TileFavorite,
        scope: ProtoLayoutScope,
    ): LayoutElementBuilders.Column {
        val modifiers = ModifiersBuilders.Modifiers.Builder()
            .setSemantics(
                ModifiersBuilders.Semantics.Builder()
                    .setContentDescription(favorite.label)
                    .build(),
            )
        launchAction(favorite.key)?.let { action ->
            modifiers.setClickable(
                ModifiersBuilders.Clickable.Builder()
                    .setId(favorite.key)
                    .setMinimumClickableWidth(dp(WearConstants.TILE_TOUCH_TARGET_DP))
                    .setMinimumClickableHeight(dp(WearConstants.TILE_TOUCH_TARGET_DP))
                    .setOnClick(action)
                    .build(),
            )
        }
        val cell = LayoutElementBuilders.Column.Builder()
            .setWidth(dp(WearConstants.TILE_CELL_WIDTH_DP))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(modifiers.build())
        favorite.iconId?.let { iconId ->
            content.iconBytes[iconId]?.let { bytes -> cell.addContent(icon(bytes, iconId, scope)) }
        }
        cell.addContent(label(favorite.label))
        return cell.build()
    }

    private fun emptyState(
        content: TileContent,
        emptyText: String,
        openLauncherText: String,
    ): LayoutElementBuilders.Column {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(safeAreaModifiers())
            .addContent(title(emptyText))
        content.launcherComponent?.let { component ->
            column.addContent(openLauncherButton(component, openLauncherText))
        }
        return column.build()
    }

    /** Keeps content off the edge of the round display. */
    private fun safeAreaModifiers(): ModifiersBuilders.Modifiers =
        ModifiersBuilders.Modifiers.Builder()
            .setPadding(
                ModifiersBuilders.Padding.Builder()
                    .setStart(dp(WearConstants.TILE_HORIZONTAL_MARGIN_DP))
                    .setEnd(dp(WearConstants.TILE_HORIZONTAL_MARGIN_DP))
                    .build(),
            )
            .build()

    private fun openLauncherButton(component: AppKey, text: String): LayoutElementBuilders.Box {
        val action = launchAction(component.flatten()) ?: return LayoutElementBuilders.Box.Builder().build()
        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(dp(WearConstants.TILE_TOUCH_TARGET_DP))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId(OPEN_LAUNCHER_ID)
                            .setMinimumClickableWidth(dp(WearConstants.TILE_TOUCH_TARGET_DP))
                            .setMinimumClickableHeight(dp(WearConstants.TILE_TOUCH_TARGET_DP))
                            .setOnClick(action)
                            .build(),
                    )
                    .setSemantics(
                        ModifiersBuilders.Semantics.Builder().setContentDescription(text).build(),
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(BUTTON_COLOR))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(dp(WearConstants.TILE_TOUCH_TARGET_DP / 2f))
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .addContent(label(text))
            .build()
    }

    private fun title(text: String): LayoutElementBuilders.Text =
        LayoutElementBuilders.Text.Builder()
            .setText(text)
            .setMaxLines(1)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(WearConstants.TILE_HEADER_SP))
                    .build(),
            )
            .build()

    private fun label(text: String): LayoutElementBuilders.Text =
        LayoutElementBuilders.Text.Builder()
            .setText(text)
            .setMaxLines(1)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(WearConstants.TILE_LABEL_SP))
                    .build(),
            )
            .build()
    private fun icon(bytes: ByteArray, iconId: String, scope: ProtoLayoutScope): LayoutElementBuilders.Image =
        LayoutElementBuilders.Image.Builder(scope)
            .setImageResource(inlineImageResource(bytes), iconId)
            .setWidth(dp(WearConstants.TILE_ICON_DP))
            .setHeight(dp(WearConstants.TILE_ICON_DP))
            .setContentScaleMode(LayoutElementBuilders.CONTENT_SCALE_MODE_FIT)
            .build()

    private fun launchAction(key: String): ActionBuilders.Action? {
        val parsed = AppKey.parse(key) ?: return null
        return ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(parsed.packageName)
                    .setClassName(parsed.className)
                    .build(),
            )
            .build()
    }

    private const val OPEN_LAUNCHER_ID = "open_launcher"
    private const val BUTTON_COLOR = 0xFF2A3A55.toInt()
    private const val TILE_GRID_COLUMNS = 2
}
