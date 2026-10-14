package com.n2.oemlauncher.wear.tile

import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.wear.config.WearConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesTileModelTest {
    private val keyA = "com.example.app/com.example.app.A"
    private val keyB = "com.example.app/com.example.app.B"
    private val keyC = "com.example.app/com.example.app.C"
    private val keyD = "com.example.app/com.example.app.D"
    private val keyE = "com.example.app/com.example.app.E"
    private val iconA = "aaaaaaaaaaaaaaaaaaaaaaaa"
    private val iconB = "bbbbbbbbbbbbbbbbbbbbbbbb"
    private val iconC = "cccccccccccccccccccccccc"

    private val catalog = WatchCatalog(
        generatedAtMillis = 1L,
        deviceName = "watch",
        sdkInt = 36,
        appVersionName = "1.0",
        display = WatchDisplay(454, 454, 320, true),
        entries = listOf(
            CatalogEntry(keyA, "Alpha", iconId = iconA),
            CatalogEntry(keyB, "Bravo", iconId = iconB),
            CatalogEntry(keyC, "Charlie"),
            CatalogEntry(keyD, "Delta"),
            CatalogEntry(keyE, "Echo"),
        ),
    )

    @Test
    fun returnsFavoritesInConfigOrder() {
        val config = LauncherConfig(favorites = listOf(keyB, keyA))
        assertEquals(listOf("Bravo", "Alpha"), FavoritesTileModel.favorites(config, catalog).map { it.label })
    }

    @Test
    fun capsFavoritesAtTheTileLimit() {
        val config = LauncherConfig(favorites = listOf(keyA, keyB, keyC, keyD, keyE))
        assertEquals(WearConstants.TILE_FAVORITES_MAX, FavoritesTileModel.favorites(config, catalog).size)
    }

    @Test
    fun dropsFavoritesThatAreHiddenOrNotInstalled() {
        val config = LauncherConfig(
            favorites = listOf(keyA, keyB),
            hidden = listOf(keyA),
        )
        assertEquals(listOf("Bravo"), FavoritesTileModel.favorites(config, catalog).map { it.label })
        assertEquals(listOf("Alpha"), FavoritesTileModel.favorites(LauncherConfig(favorites = listOf(keyA, "com.gone/gone.Main")), catalog).map { it.label })
    }

    @Test
    fun prefersTheCustomIconOverride() {
        val config = LauncherConfig(favorites = listOf(keyA), iconOverrides = mapOf(keyA to iconC))
        assertEquals(iconC, FavoritesTileModel.favorites(config, catalog).single().iconId)
        assertEquals(iconA, FavoritesTileModel.favorites(LauncherConfig(favorites = listOf(keyA)), catalog).single().iconId)
    }

    @Test
    fun emptyWithoutFavoritesOrCatalog() {
        assertTrue(FavoritesTileModel.favorites(LauncherConfig(favorites = listOf(keyA)), null).isEmpty())
        assertTrue(FavoritesTileModel.favorites(LauncherConfig(), catalog).isEmpty())
    }

    @Test
    fun versionChangesWithFavoritesOrIconAvailability() {
        val favorites = FavoritesTileModel.favorites(LauncherConfig(favorites = listOf(keyA)), catalog)
        val withIcon = FavoritesTileModel.resourcesVersion(favorites, setOf(iconA))
        val withoutIcon = FavoritesTileModel.resourcesVersion(favorites, emptySet())
        val reordered = FavoritesTileModel.resourcesVersion(
            FavoritesTileModel.favorites(LauncherConfig(favorites = listOf(keyB, keyA)), catalog),
            setOf(iconA),
        )

        assertEquals(withIcon, FavoritesTileModel.resourcesVersion(favorites, setOf(iconA)))
        assertNotEquals(withIcon, withoutIcon)
        assertNotEquals(withIcon, reordered)
    }
}
