package com.n2.oemlauncher.wear.domain

import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.SortMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherArrangerTest {
    @Test
    fun favoritesLeadAndAlphabeticalSortIsStable() {
        val entries = listOf(
            CatalogEntry("com.example/z", "Zed"),
            CatalogEntry("com.example/a", "Alpha"),
        )
        val config = LauncherConfig(sortMode = SortMode.ALPHABETICAL, favorites = listOf("com.example/z"))
        assertEquals(listOf("com.example/z", "com.example/a"), LauncherArranger.arrange(entries, config).map { it.key })
    }
}
