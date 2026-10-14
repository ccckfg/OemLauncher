package com.n2.oemlauncher.wear.sync

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadCappedTest {

    @Test
    fun returnsBytesWhenWithinTheLimit() {
        val bytes = ByteArray(1024) { it.toByte() }
        assertArrayEquals(bytes, readCapped(ByteArrayInputStream(bytes), 1024))
    }

    @Test
    fun dropsStreamsLongerThanTheLimit() {
        assertNull(readCapped(ByteArrayInputStream(ByteArray(2048)), 1024))
    }

    @Test
    fun returnsAnEmptyArrayForAnEmptyStream() {
        assertArrayEquals(ByteArray(0), readCapped(ByteArrayInputStream(ByteArray(0)), 16))
    }
}
