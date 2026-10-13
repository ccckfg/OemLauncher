package com.n2.oemlauncher.shared.protocol

/** Hard limits applied on both ends of the sync channel. */
object ProtocolLimits {
    /** The platform caps DataItem payloads at 100 KB; keep headroom for the DataMap itself. */
    const val MAX_PAYLOAD_BYTES = 90 * 1024

    /** Upper bound after gzip inflation; guards against compression bombs. */
    const val MAX_INFLATED_BYTES = 2 * 1024 * 1024

    const val MAX_CATALOG_ENTRIES = 600

    /** One encoded icon (PNG or WebP). */
    const val MAX_ICON_BYTES = 128 * 1024

    /** Edge length of the default icons the watch sends for the phone editor and preview. */
    const val CATALOG_ICON_PX = 128

    /** Edge length of custom icons produced by the phone. */
    const val CUSTOM_ICON_PX = 192
}
