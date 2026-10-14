package com.n2.oemlauncher.wear.data

import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IconFileStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun storesReadsAndValidatesContentAddressedIcons() {
        val store = IconFileStore(folder.root)
        val bytes = "icon-bytes".encodeToByteArray()
        val iconId = IconId.of(bytes)
        assertNull(store.get(iconId))
        assertFalse(store.isValidStored(iconId))

        store.put(iconId, bytes)

        assertArrayEquals(bytes, store.get(iconId))
        assertTrue(store.isValidStored(iconId))
    }

    @Test
    fun rejectsStoredContentThatDoesNotMatchItsId() {
        val store = IconFileStore(folder.root)
        val iconId = IconId.of("wanted".encodeToByteArray())
        store.put(iconId, "other".encodeToByteArray())
        assertFalse(store.isValidStored(iconId))
    }

    @Test
    fun replacesAnExistingFileAndIgnoresEmptyWrites() {
        val store = IconFileStore(folder.root)
        val iconId = IconId.of("first".encodeToByteArray())
        store.put(iconId, "first".encodeToByteArray())
        store.put(iconId, "second".encodeToByteArray())
        assertArrayEquals("second".encodeToByteArray(), store.get(iconId))

        store.put(iconId, ByteArray(0))

        assertArrayEquals("second".encodeToByteArray(), store.get(iconId))
    }

    @Test
    fun treatsOversizedStoredContentAsInvalid() {
        val store = IconFileStore(folder.root)
        val oversized = ByteArray(ProtocolLimits.MAX_ICON_BYTES + 1)
        val iconId = IconId.of(oversized)
        store.put(iconId, oversized)
        assertFalse(store.isValidStored(iconId))
    }
}
