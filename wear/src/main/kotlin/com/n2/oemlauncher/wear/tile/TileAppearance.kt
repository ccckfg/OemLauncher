package com.n2.oemlauncher.wear.tile

import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import com.n2.oemlauncher.shared.design.ColorMath
import com.n2.oemlauncher.shared.design.Palettes
import com.n2.oemlauncher.wear.config.WearConstants

/** ProtoLayout has no blur; every surface is opaque and already composited over Mocha. */
internal object TileAppearance {
    val background = Palettes.Mocha.background.toInt()
    val glass = ColorMath.compositeOver(Palettes.Mocha.glass, Palettes.Mocha.background).toInt()
    val button = Palettes.Mocha.accent.toInt()

    fun root(content: LayoutElementBuilders.LayoutElement): LayoutElementBuilders.Box =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(ModifiersBuilders.Background.Builder().setColor(argb(background)).build())
                    .build(),
            )
            .addContent(content)
            .build()

    fun title(text: String): LayoutElementBuilders.Text = text(text, WearConstants.TILE_HEADER_SP)

    fun label(text: String, onAccent: Boolean = false): LayoutElementBuilders.Text =
        text(text, WearConstants.TILE_LABEL_SP, onAccent)

    private fun text(text: String, size: Float, onAccent: Boolean = false): LayoutElementBuilders.Text =
        LayoutElementBuilders.Text.Builder()
            .setText(text)
            .setMaxLines(1)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size))
                    .setColor(argb((if (onAccent) Palettes.Mocha.onAccent else Palettes.Mocha.textPrimary).toInt()))
                    .build(),
            )
            .build()
}
