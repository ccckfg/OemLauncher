package com.n2.oemlauncher.shared.design

/**
 * Color roles of the mocha glass language (docs/design/README.md), packed as 0xAARRGGBB.
 * Both apps convert these to their own color types, so the phone preview paints the watch
 * with exactly the colors the watch uses.
 */
class DesignPalette(
    val background: Long,
    /** Warm light pooled behind content; drawn as a soft radial gradient, never as a flat fill. */
    val backgroundGlow: Long,
    /** Faint cool counterweight to the warm glow, used sparingly at low alpha. */
    val coolGlow: Long,
    /** Translucent surface for rows, cards and inactive controls. */
    val glass: Long,
    /** Denser glass for controls that sit on top of another glass surface. */
    val glassStrong: Long,
    /** Top-edge highlight that makes glass read as a physical pane. */
    val rimLight: Long,
    /** Champagne: selected and primary-action fills. */
    val accent: Long,
    val accentLight: Long,
    val accentDeep: Long,
    /** Text and glyphs drawn on [accent]. */
    val onAccent: Long,
    /** Champagne as text (section headers); darker than [accent] where needed to stay legible. */
    val accentText: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    /** Hints and disabled text only; meets 3:1, not 4.5:1. */
    val textTertiary: Long,
    /** Apricot: tiny indicators (focus dot, progress), never large fills. */
    val highlight: Long,
    val positive: Long,
    val negative: Long,
)

object Palettes {
    /** Dark palette; the watch always uses it, the phone uses it in dark mode. */
    val Mocha = DesignPalette(
        background = 0xFF140E0B,
        backgroundGlow = 0xFF3A2818,
        coolGlow = 0xFF2B3446,
        glass = 0xA6302620,
        glassStrong = 0xCC51463F,
        rimLight = 0x38FFF3E6,
        accent = 0xFFD6BFA8,
        accentLight = 0xFFE6D2BE,
        accentDeep = 0xFFBEA591,
        onAccent = 0xFF2E1D0D,
        accentText = 0xFFBEA591,
        textPrimary = 0xFFFFF7EE,
        textSecondary = 0xFFD9CCC0,
        textTertiary = 0xFFA8998C,
        highlight = 0xFFF8BA63,
        positive = 0xFFB9C98A,
        negative = 0xFFE8846B,
    )

    /** Light palette for the phone in light mode. */
    val Latte = DesignPalette(
        background = 0xFFF3EBE2,
        backgroundGlow = 0xFFE8D6C3,
        coolGlow = 0xFFD5DCE8,
        glass = 0xB3FFFFFF,
        glassStrong = 0xE6FFFFFF,
        rimLight = 0xCCFFFFFF,
        accent = 0xFFD2B79C,
        accentLight = 0xFFE3CDB6,
        accentDeep = 0xFFB89B80,
        onAccent = 0xFF2E1D0D,
        accentText = 0xFF7E624A,
        textPrimary = 0xFF2A1D14,
        textSecondary = 0xFF5E4E43,
        textTertiary = 0xFF857467,
        highlight = 0xFFB86A1F,
        positive = 0xFF5E7335,
        negative = 0xFFA9482F,
    )
}
