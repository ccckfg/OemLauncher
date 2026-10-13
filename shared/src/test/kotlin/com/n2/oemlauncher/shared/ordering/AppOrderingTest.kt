package com.n2.oemlauncher.shared.ordering

import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.SortMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppOrderingTest {

    private val alpha = entry("alpha", "alpha", launches = 1, last = 10)
    private val bravo = entry("bravo", "Bravo", launches = 9, last = 5)
    private val charlie = entry("charlie", "charlie", launches = 9, last = 50)
    private val delta = entry("delta", "Delta", launches = 0, last = 0)
    private val entries = listOf(delta, charlie, bravo, alpha)
    private val comparator = AppOrdering.labelComparator(Locale.ENGLISH)

    @Test
    fun favoritesLeadInConfigOrderAndHiddenAreDropped() {
        val config = LauncherConfig(
            favorites = listOf(charlie.key, alpha.key),
            hidden = listOf(delta.key),
            sortMode = SortMode.ALPHABETICAL,
        )
        val arranged = AppOrdering.arrange(entries, config, comparator)
        assertEquals(listOf(charlie, alpha), arranged.favorites)
        assertEquals(listOf(bravo), arranged.others)
        assertEquals(listOf(charlie, alpha, bravo), arranged.all)
    }

    @Test
    fun alphabeticalIgnoresCase() {
        val arranged = AppOrdering.arrange(entries, LauncherConfig(sortMode = SortMode.ALPHABETICAL), comparator)
        assertEquals(listOf(alpha, bravo, charlie, delta), arranged.others)
    }

    @Test
    fun customOrderAppendsNewAppsAlphabetically() {
        val config = LauncherConfig(sortMode = SortMode.CUSTOM, order = listOf(delta.key, bravo.key, "gone/gone.App"))
        val arranged = AppOrdering.arrange(entries, config, comparator)
        assertEquals(listOf(delta, bravo, alpha, charlie), arranged.others)
    }

    @Test
    fun mostUsedBreaksTiesByRecency() {
        val arranged = AppOrdering.arrange(entries, LauncherConfig(sortMode = SortMode.MOST_USED), comparator)
        assertEquals(listOf(charlie, bravo, alpha, delta), arranged.others)
    }

    @Test
    fun recentlyUsedSortsByLastLaunch() {
        val arranged = AppOrdering.arrange(entries, LauncherConfig(sortMode = SortMode.RECENTLY_USED), comparator)
        assertEquals(listOf(charlie, alpha, bravo, delta), arranged.others)
    }

    @Test
    fun hiddenFavoriteIsNotShown() {
        val config = LauncherConfig(favorites = listOf(alpha.key), hidden = listOf(alpha.key))
        val arranged = AppOrdering.arrange(entries, config, comparator)
        assertEquals(emptyList<CatalogEntry>(), arranged.favorites)
        assertEquals(3, arranged.others.size)
    }

    private fun entry(name: String, label: String, launches: Int, last: Long) = CatalogEntry(
        key = "com.$name/com.$name.Main",
        label = label,
        launchCount = launches,
        lastLaunchedMillis = last,
    )
}
