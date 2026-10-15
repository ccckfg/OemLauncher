package com.n2.oemlauncher.wear.domain

import com.n2.oemlauncher.shared.model.WatchCatalog

/**
 * Content signature of a catalog: changes exactly when the phone would have to be told about
 * new content (apps added or removed, labels or default icons changed). The volatile timestamp
 * is deliberately excluded.
 */
object CatalogSignature {
    fun of(catalog: WatchCatalog): String =
        catalog.entries.joinToString("\u0000") { entry ->
            "${entry.key}\u0001${entry.label}\u0001${entry.iconId.orEmpty()}\u0001${entry.isSystem}"
        }
}
