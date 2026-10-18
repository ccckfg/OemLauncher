package com.n2.oemlauncher.wear.tile

import com.n2.oemlauncher.shared.design.ColorMath
import com.n2.oemlauncher.shared.design.Palettes
import org.junit.Assert.assertEquals
import org.junit.Test

class TileAppearanceTest {
    @Test fun tileSurfacesAreOpaqueMochaComposites() {
        assertEquals(Palettes.Mocha.background.toInt(), TileAppearance.background)
        assertEquals(ColorMath.compositeOver(Palettes.Mocha.glass, Palettes.Mocha.background).toInt(), TileAppearance.glass)
        assertEquals(255, TileAppearance.glass ushr 24)
        assertEquals(Palettes.Mocha.accent.toInt(), TileAppearance.button)
    }

    @Test fun tileTextUsesPaletteColorsInsteadOfHostDefaults() {
        assertEquals(Palettes.Mocha.textPrimary.toInt(), TileAppearance.title("Favorites").fontStyle!!.color!!.argb)
        assertEquals(Palettes.Mocha.textPrimary.toInt(), TileAppearance.label("Clock").fontStyle!!.color!!.argb)
        assertEquals(Palettes.Mocha.onAccent.toInt(), TileAppearance.label("Open", true).fontStyle!!.color!!.argb)
    }

    @Test fun rootFillsTileWithMochaAndCentersItsContent() {
        val root = TileAppearance.root(TileAppearance.title("Favorites"))
        assertEquals(TileAppearance.background, root.modifiers!!.background!!.color!!.argb)
        assertEquals(1, root.contents.size)
    }
}
