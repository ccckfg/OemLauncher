package com.n2.oemlauncher.shared.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Thrown when an inflated payload grows past its limit. */
internal class PayloadTooLargeException(size: Int) :
    IOException("Inflated payload exceeds the limit at $size bytes")

internal object GzipCodec {
    private const val BUFFER_SIZE = 8 * 1024
    private const val MIN_OUTPUT_CAPACITY = 64

    fun compress(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(bytes.size / 4 + MIN_OUTPUT_CAPACITY)
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    /** Inflates [bytes], failing fast with [PayloadTooLargeException] beyond [maxBytes]. */
    fun decompress(bytes: ByteArray, maxBytes: Int): ByteArray {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val output = ByteArrayOutputStream(minOf(bytes.size * 4, maxBytes))
            val buffer = ByteArray(BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw PayloadTooLargeException(total)
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }
}
