package com.n2.oemlauncher.shared.protocol

import java.security.MessageDigest

/**
 * Content address of encoded icon bytes. Identical images share one id, so each device stores
 * and transfers an icon once no matter how many apps use it.
 */
object IconId {
    const val LENGTH = 24

    private val PATTERN = Regex("^[0-9a-f]{$LENGTH}$")
    private val HEX = "0123456789abcdef".toCharArray()

    fun of(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val out = CharArray(LENGTH)
        for (i in 0 until LENGTH / 2) {
            val value = digest[i].toInt() and 0xFF
            out[i * 2] = HEX[value ushr 4]
            out[i * 2 + 1] = HEX[value and 0x0F]
        }
        return String(out)
    }

    fun isValid(id: String): Boolean = PATTERN.matches(id)
}
