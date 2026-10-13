package com.n2.oemlauncher.mobile.iconpack

import com.n2.oemlauncher.mobile.config.ImportLimits
import java.io.ByteArrayOutputStream
import java.io.InputStream

// InputStream.readNBytes is only available on newer Android versions than our minSdk.
fun InputStream.readBounded(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(ImportLimits.BUFFER_BYTES)
    while (output.size() <= limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit + 1 - output.size()))
        if (count < 0) break
        if (count == 0) continue
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
