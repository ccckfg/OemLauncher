package com.n2.oemlauncher.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.n2.oemlauncher.mobile.data.IconStore
import com.n2.oemlauncher.mobile.iconpack.CropTransform
import com.n2.oemlauncher.mobile.iconpack.IconImportException
import com.n2.oemlauncher.mobile.iconpack.IconProcessor
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconProcessorTest {
    @get:Rule val temporary = TemporaryFolder()
    private val processor = IconProcessor()
    private fun source() = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

    @Test fun cropsToProtocolSizeAndRoundTransparency() {
        val bytes = processor.crop(source(), CropTransform(), circle = true)
        val output = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(ProtocolLimits.CUSTOM_ICON_PX, output.width)
        assertEquals(ProtocolLimits.CUSTOM_ICON_PX, output.height)
        assertEquals(0, Color.alpha(output.getPixel(0, 0)))
        assertTrue(bytes.size <= ProtocolLimits.MAX_ICON_BYTES)
    }
    @Test fun normalizingIsDeterministicAndContentAddressed() {
        val bytes = processor.crop(source(), CropTransform(), circle = false)
        assertArrayEquals(processor.normalize(bytes), processor.normalize(bytes))
        assertEquals(IconId.of(bytes), IconId.of(bytes.copyOf()))
    }
    @Test fun storedBytesKeepExactlyTheSameId() = runTest {
        val bytes = processor.crop(source(), CropTransform(), circle = false)
        val store = IconStore(temporary.root)
        val id = store.put(bytes)
        assertEquals(IconId.of(bytes), id)
        assertArrayEquals(bytes, store.bytes(id))
    }
    @Test fun rejectsWrongAssetIdAndOversizeAssets() = runTest {
        val store = IconStore(temporary.root)
        try {
            store.put(byteArrayOf(1), IconId.of(byteArrayOf(2)))
            fail("Mismatched digest accepted")
        } catch (_: IllegalArgumentException) { }
        try {
            store.put(ByteArray(ProtocolLimits.MAX_ICON_BYTES + 1))
            fail("Oversize accepted")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun cropConstraintsNeverExposeBlankEdges() {
        val transform = CropTransform(2f, 100f, -100f).constrained(320, 240)
        assertEquals(0.5f, transform.y.absoluteValue, 0.001f)
        assertEquals((320f / 240 * 2 - 1) / 2, transform.x, 0.001f)
        assertEquals(1f, CropTransform(-3f).constrained(320, 240).zoom, 0f)
    }
    @Test fun rejectsInvalidImageBytes() {
        assertThrows(IconImportException::class.java) { processor.normalize(byteArrayOf(1, 2, 3)) }
    }
    @Test fun noisyTransparentIconsStillFitTheProtocolBudget() {
        val side = ProtocolLimits.CUSTOM_ICON_PX
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val random = java.util.Random(42)
        val pixels = IntArray(side * side) { random.nextInt() }
        bitmap.setPixels(pixels, 0, side, 0, 0, side, side)
        val bytes = processor.crop(bitmap, CropTransform(), circle = false)
        assertTrue(bytes.size <= ProtocolLimits.MAX_ICON_BYTES)
    }
    private val Float.absoluteValue get() = kotlin.math.abs(this)
}
