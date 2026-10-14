package com.n2.oemlauncher.shared.protocol

/**
 * Wearable Data Layer paths. Everything lives under one versioned root so a future protocol
 * revision can run side by side with this one.
 */
object SyncPaths {
    const val ROOT = "/oemlauncher/v1"

    /** Watch-owned DataItem: gzip WatchCatalog payload plus one Asset per default icon. */
    const val CATALOG = "$ROOT/catalog"

    /** Phone-owned DataItem: gzip LauncherConfig payload plus one Asset per custom icon. */
    const val CONFIG_DESIRED = "$ROOT/config/desired"

    /** Watch-owned DataItem: gzip WatchConfigState payload (applied config + apply result). */
    const val CONFIG_STATE = "$ROOT/config/state"

    /** Message phone -> watch: republish the catalog now (pull to refresh, first pairing). */
    const val MESSAGE_REQUEST_CATALOG = "$ROOT/message/request-catalog"
}

/** Keys inside the DataMaps of the items above. */
object SyncKeys {
    /** Int: protocol schema of the payload, see ProtocolVersion. */
    const val SCHEMA = "schema"

    /** ByteArray: gzip JSON produced by PayloadCodec. */
    const val PAYLOAD = "payload"

    /**
     * Long, desired items only: the config revision, duplicated outside the payload so the watch
     * can tie a schema or decode rejection to the revision the phone is waiting for. Optional,
     * because phones built before it existed never write it.
     */
    const val REVISION = "revision"

    private const val ICON_ASSET_PREFIX = "icon_"

    fun iconAssetKey(iconId: String): String = ICON_ASSET_PREFIX + iconId

    /** Icon id encoded in a DataMap key, or null when the key is not a valid icon asset key. */
    fun iconIdFromAssetKey(key: String): String? =
        if (key.startsWith(ICON_ASSET_PREFIX)) {
            key.removePrefix(ICON_ASSET_PREFIX).takeIf(IconId::isValid)
        } else {
            null
        }
}

/** Capability names advertised by each app. They must match res/values/wear.xml. */
object Capabilities {
    const val WATCH_APP = "oemlauncher_watch_app"
    const val PHONE_APP = "oemlauncher_phone_app"
}
