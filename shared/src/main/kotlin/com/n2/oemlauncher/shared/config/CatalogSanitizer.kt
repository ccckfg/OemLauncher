package com.n2.oemlauncher.shared.config

import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.ProtocolLimits

/** Canonical keys, unique entries, valid icon ids and bounded sizes for a received catalog. */
object CatalogSanitizer {

    fun sanitize(catalog: WatchCatalog): WatchCatalog {
        val seen = HashSet<String>()
        val entries = catalog.entries.asSequence()
            .mapNotNull { entry ->
                val key = AppKey.canonical(entry.key) ?: return@mapNotNull null
                entry.copy(
                    key = key,
                    label = entry.label.take(LauncherDefaults.MAX_LABEL_LENGTH),
                    iconId = entry.iconId?.takeIf(IconId::isValid),
                    launchCount = entry.launchCount.coerceAtLeast(0),
                    lastLaunchedMillis = entry.lastLaunchedMillis.coerceAtLeast(0),
                )
            }
            .filter { seen.add(it.key) }
            .take(ProtocolLimits.MAX_CATALOG_ENTRIES)
            .toList()
        return catalog.copy(
            deviceName = catalog.deviceName.take(LauncherDefaults.MAX_LABEL_LENGTH),
            entries = entries,
        )
    }
}
