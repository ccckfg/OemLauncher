package com.n2.oemlauncher.shared.model

import com.n2.oemlauncher.shared.config.LauncherDefaults
import kotlinx.serialization.Serializable

/**
 * The launcher configuration both apps edit.
 *
 * All lists hold canonical [AppKey] strings. [revision] orders competing edits from the phone
 * and the watch: the higher revision wins (see docs/sync-protocol.md). [iconOverrides] maps an
 * app key to a content-addressed icon id (see IconId).
 */
@Serializable
data class LauncherConfig(
    val revision: Long = 0,
    val editedAtMillis: Long = 0,
    val origin: ConfigOrigin = ConfigOrigin.WATCH,
    val layout: LayoutSettings = LayoutSettings(),
    val sortMode: SortMode = LauncherDefaults.SORT_MODE,
    val order: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val iconOverrides: Map<String, String> = emptyMap(),
)
