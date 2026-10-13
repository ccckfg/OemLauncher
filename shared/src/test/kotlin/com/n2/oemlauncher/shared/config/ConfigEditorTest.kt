package com.n2.oemlauncher.shared.config

import com.n2.oemlauncher.shared.model.ConfigOrigin
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class ConfigEditorTest {

    private val keys = (1..10).map { "com.app$it/com.app$it.Main" }

    @Test
    fun toggleFavoriteAddsRemovesAndRespectsLimit() {
        var config = LauncherConfig(hidden = listOf(keys[0]))
        val (added, addResult) = ConfigEditor.toggleFavorite(config, keys[0])
        assertEquals(FavoriteToggle.ADDED, addResult)
        assertEquals(listOf(keys[0]), added.favorites)
        assertFalse("favoriting unhides", keys[0] in added.hidden)

        val (removed, removeResult) = ConfigEditor.toggleFavorite(added, keys[0])
        assertEquals(FavoriteToggle.REMOVED, removeResult)
        assertEquals(emptyList<String>(), removed.favorites)

        config = LauncherConfig(favorites = keys.take(LauncherDefaults.MAX_FAVORITES))
        val (unchanged, limitResult) = ConfigEditor.toggleFavorite(config, keys.last())
        assertEquals(FavoriteToggle.LIMIT_REACHED, limitResult)
        assertSame(config, unchanged)
    }

    @Test
    fun hidingRemovesFavorite() {
        val config = LauncherConfig(favorites = listOf(keys[1]))
        val hidden = ConfigEditor.setHidden(config, keys[1], hidden = true)
        assertEquals(listOf(keys[1]), hidden.hidden)
        assertEquals(emptyList<String>(), hidden.favorites)
        assertEquals(emptyList<String>(), ConfigEditor.setHidden(hidden, keys[1], hidden = false).hidden)
    }

    @Test
    fun moveAppKeepsRememberedKeysAndSwitchesToCustom() {
        val config = LauncherConfig(sortMode = SortMode.ALPHABETICAL, order = listOf(keys[9], keys[0]))
        val visible = listOf(keys[0], keys[1], keys[2])
        val moved = ConfigEditor.moveApp(config, visible, from = 0, to = 2)
        assertEquals(listOf(keys[1], keys[2], keys[0], keys[9]), moved.order)
        assertEquals(SortMode.CUSTOM, moved.sortMode)
        assertSame(config, ConfigEditor.moveApp(config, visible, from = 5, to = 0))
    }

    @Test
    fun updateLayoutIsSanitized() {
        val config = ConfigEditor.updateLayout(LauncherConfig()) {
            it.copy(mode = LayoutMode.GRID, gridColumns = 99, fisheyeStrength = -3f)
        }
        assertEquals(LayoutMode.GRID, config.layout.mode)
        assertEquals(LauncherDefaults.GRID_COLUMNS_MAX, config.layout.gridColumns)
        assertEquals(LauncherDefaults.FISHEYE_MIN, config.layout.fisheyeStrength, 0f)
    }

    @Test
    fun pruneAndStamp() {
        val config = LauncherConfig(
            order = keys.take(3),
            favorites = listOf(keys[0]),
            iconOverrides = mapOf(keys[2] to "0".repeat(24)),
        )
        val pruned = ConfigEditor.pruneTo(config, setOf(keys[0], keys[1]))
        assertEquals(keys.take(2), pruned.order)
        assertEquals(emptyMap<String, String>(), pruned.iconOverrides)
        val stamped = ConfigEditor.stamp(pruned, revision = 7, origin = ConfigOrigin.PHONE, nowMillis = 42)
        assertEquals(7L, stamped.revision)
        assertEquals(ConfigOrigin.PHONE, stamped.origin)
        assertEquals(42L, stamped.editedAtMillis)
    }
}
