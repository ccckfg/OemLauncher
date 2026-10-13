package com.n2.oemlauncher.shared.ordering

import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.SortMode
import java.text.Collator
import java.util.Locale

/** Apps exactly as the launcher shows them: favorites first (in config order), then the rest. */
data class ArrangedApps(val favorites: List<CatalogEntry>, val others: List<CatalogEntry>) {
    val all: List<CatalogEntry> = favorites + others
}

/**
 * The single ordering rule used by the watch UI and the phone preview, so both always agree.
 * Hidden apps are dropped, favorites lead, and new apps in custom mode are appended
 * alphabetically after the remembered order.
 */
object AppOrdering {

    fun arrange(
        entries: List<CatalogEntry>,
        config: LauncherConfig,
        labelComparator: Comparator<String> = labelComparator(),
    ): ArrangedApps {
        val hidden = config.hidden.toHashSet()
        val visible = LinkedHashMap<String, CatalogEntry>(entries.size * 2)
        for (entry in entries) {
            if (entry.key !in hidden) visible.putIfAbsent(entry.key, entry)
        }
        val favorites = config.favorites.asSequence()
            .mapNotNull { visible[it] }
            .distinct()
            .take(LauncherDefaults.MAX_FAVORITES)
            .toList()
        val favoriteKeys = favorites.mapTo(HashSet()) { it.key }
        val rest = visible.values.filter { it.key !in favoriteKeys }
        val byLabel = compareBy<CatalogEntry, String>(labelComparator) { it.label }
            .thenBy { it.key }
        val others = when (config.sortMode) {
            SortMode.CUSTOM -> customOrder(rest, config.order, byLabel)
            SortMode.ALPHABETICAL -> rest.sortedWith(byLabel)
            SortMode.MOST_USED -> rest.sortedWith(
                compareByDescending<CatalogEntry> { it.launchCount }
                    .thenByDescending { it.lastLaunchedMillis }
                    .then(byLabel),
            )
            SortMode.RECENTLY_USED -> rest.sortedWith(
                compareByDescending<CatalogEntry> { it.lastLaunchedMillis }.then(byLabel),
            )
        }
        return ArrangedApps(favorites, others)
    }

    /**
     * Locale-aware, case-insensitive label ordering (pinyin order for Chinese on Android).
     * Collator is not thread-safe, so every call returns a comparator with its own instance.
     */
    fun labelComparator(locale: Locale = Locale.getDefault()): Comparator<String> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        return Comparator { a, b -> collator.compare(a, b) }
    }

    private fun customOrder(
        rest: List<CatalogEntry>,
        order: List<String>,
        fallback: Comparator<CatalogEntry>,
    ): List<CatalogEntry> {
        val position = HashMap<String, Int>(order.size * 2)
        order.forEachIndexed { index, key -> position.putIfAbsent(key, index) }
        val (known, unknown) = rest.partition { it.key in position }
        return known.sortedBy { position.getValue(it.key) } + unknown.sortedWith(fallback)
    }
}
