package com.n2.oemlauncher.shared.protocol

/** Schema of every payload; bump CURRENT on breaking changes and keep old readers working. */
object ProtocolVersion {
    const val CURRENT = 1
    const val MIN_SUPPORTED = 1

    fun isSupported(schema: Int): Boolean = schema in MIN_SUPPORTED..CURRENT
}
