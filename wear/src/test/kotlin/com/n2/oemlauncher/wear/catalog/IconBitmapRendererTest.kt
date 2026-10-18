package com.n2.oemlauncher.wear.catalog

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import com.n2.oemlauncher.shared.design.ColorMath
import com.n2.oemlauncher.shared.design.Palettes
import com.n2.oemlauncher.shared.model.IconShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconBitmapRendererTest {
    private fun adaptive() = AdaptiveIconDrawable(
        ColorDrawable(Color.RED), ColorDrawable(Color.TRANSPARENT),
        InsetDrawable(ColorDrawable(Color.WHITE), 35),
    )

    @Test fun themedIconsFollowCircleAndRoundedSquareMasks() {
        val circle = IconBitmapRenderer.themed(adaptive(), 100, IconShape.CIRCLE)!!
        val square = IconBitmapRenderer.themed(adaptive(), 100, IconShape.SQUIRCLE)!!
        assertEquals(0, Color.alpha(circle.getPixel(5, 20)))
        assertEquals(255, Color.alpha(square.getPixel(5, 20)))
        assertEquals(0, Color.alpha(square.getPixel(0, 0)))
        assertEquals(Palettes.Mocha.textPrimary.toInt(), square.getPixel(50, 50))
        assertEquals(
            ColorMath.compositeOver(Palettes.Mocha.glass, Palettes.Mocha.background).toInt(),
            square.getPixel(50, 10),
        )
    }

    @Test fun originalThemeUsesCircularGlassWithoutCroppingNativeOriginals() {
        val themed = IconBitmapRenderer.themed(adaptive(), 100, IconShape.ORIGINAL)!!
        assertEquals(0, Color.alpha(themed.getPixel(5, 20)))
        val original = IconBitmapRenderer.render(ColorDrawable(Color.RED), 100, IconShape.ORIGINAL)
        assertEquals(Color.RED, original.getPixel(0, 0))
    }

    @Test fun adaptiveRoundedSquareBypassesPlatformCircularMask() {
        val square = IconBitmapRenderer.render(adaptive(), 100, IconShape.SQUIRCLE)
        val circle = IconBitmapRenderer.render(adaptive(), 100, IconShape.CIRCLE)
        assertEquals(Color.RED, square.getPixel(5, 20))
        assertEquals(0, Color.alpha(circle.getPixel(5, 20)))
    }

    @Test fun missingMonochromeLayerFallsBackWithoutMakingAThemedBitmap() {
        val drawable = AdaptiveIconDrawable(ColorDrawable(Color.RED), ColorDrawable(Color.TRANSPARENT))
        assertNull(IconBitmapRenderer.themed(drawable, 100, IconShape.CIRCLE))
        assertNull(IconBitmapRenderer.themed(ColorDrawable(Color.RED), 100, IconShape.CIRCLE))
    }

    @Test fun monochromeMutatesBeforeTintAndDoesNotTintSharedSibling() {
        val layer = MutationCheckingDrawable()
        val sibling = layer.constantState!!.newDrawable()
        val drawable = AdaptiveIconDrawable(ColorDrawable(Color.RED), ColorDrawable(Color.TRANSPARENT), layer)
        IconBitmapRenderer.themed(drawable, 100, IconShape.CIRCLE)
        assertTrue(layer.mutated)
        assertEquals(Color.WHITE, IconBitmapRenderer.render(sibling, 100, IconShape.ORIGINAL).getPixel(50, 50))
        assertNotEquals(Color.WHITE, IconBitmapRenderer.render(layer, 100, IconShape.ORIGINAL).getPixel(50, 50))
    }

    private class MutationCheckingDrawable : ColorDrawable(Color.WHITE) {
        var mutated = false
        override fun mutate(): android.graphics.drawable.Drawable {
            mutated = true
            return super.mutate()
        }
        override fun setTint(tintColor: Int) {
            check(mutated)
            super.setTint(tintColor)
        }
    }
}
