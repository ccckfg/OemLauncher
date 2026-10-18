package com.n2.oemlauncher.mobile.config

/**
 * Shapes, sizes and timings of the phone's mocha glass UI (docs/design/README.md sections 4 and 8).
 * Colors live in `shared`, the type scale in [TypeTokens].
 */
object GlassTokens {
    /** Corner radii, in dp. Capsules use 50% of their height instead. */
    const val CARD_CORNER = 28
    const val TILE_CORNER = 24
    const val ROW_CORNER = 20
    const val SHEET_CORNER = 28

    const val TOP_BAR_HEIGHT = 48
    const val CARD_PADDING = 16
    const val CHIP_HEIGHT = 34
    const val CHIP_PADDING = 14
    const val ROW_HEIGHT = 64
    const val SETTING_ROW = 52
    const val SEGMENT_HEIGHT = 40
    const val SEGMENT_PADDING = 3
    const val SEGMENT_GAP = 2
    const val TILE_HEIGHT = 132
    const val TILE_ICON = 40
    const val BUTTON_HEIGHT = 48
    const val STATUS_DOT = 8

    const val SWITCH_WIDTH = 48
    const val SWITCH_HEIGHT = 28
    const val SWITCH_THUMB = 20
    const val SWITCH_INSET = 4
    const val SLIDER_TRACK = 6
    const val SLIDER_THUMB = 22

    /** Drag lifts a row: shadow plus a small scale, like a card picked off the table. */
    const val DRAG_SCALE = 1.02f

    /**
     * Raised glass. The shadow is a stack of bands clipped to the outside of the shape, so
     * nothing is ever painted under the translucent fill; a real blur behind the pane would
     * show through it as a grey rectangle. More bands mean a smoother ramp.
     */
    const val SHADOW_SPREAD_DP = 14
    const val SHADOW_DROP_DP = 3
    const val SHADOW_LAYERS = 20

    /** Hero preview on the home page and the smaller one on the appearance page. */
    const val HERO_PREVIEW = 236
    const val SMALL_PREVIEW = 168
    const val PREVIEW_MAX = 280
    const val PREVIEW_MIN = 216
    const val PREVIEW_FOOTER = 140
    const val BEZEL_RING = 2
    const val PREVIEW_BORDER = 6

    /** Scrim behind a dialog or bottom sheet when the window cannot blur what is behind it. */
    const val SCRIM_ALPHA = 0.7f

    /** Ambient glows of the page background, as fractions of the page size. */
    const val WARM_GLOW_X = 0.12f
    const val WARM_GLOW_Y = 0.06f
    const val WARM_GLOW_RADIUS = 0.47f
    const val COOL_GLOW_X = 1f
    const val COOL_GLOW_Y = 0.46f
    const val COOL_GLOW_RADIUS = 0.29f

    /** Champagne halo drawn around the hero card, and the alpha it is drawn at. */
    const val HERO_HALO_ALPHA = 0.22f
    const val HERO_HALO_RADIUS = 1.5f

    /** Motion (docs/design/README.md section 8), in milliseconds. */
    const val MOTION_SHORT = 150
    const val MOTION_STANDARD = 260
    const val MOTION_EMPHASIS = 380

    /** Spring for drag release: damping 0.8, medium stiffness. */
    const val SPRING_DAMPING = 0.8f
    const val SPRING_STIFFNESS = 800f
}

/** Phone type scale, in sp. Weights and letter spacing live in the Typography object. */
object TypeTokens {
    const val DISPLAY_SP = 28
    const val DISPLAY_LINE = 34
    const val TITLE_SP = 20
    const val TITLE_LINE = 26
    const val SUBTITLE_SP = 16
    const val SUBTITLE_LINE = 22
    const val BODY_SP = 15
    const val BODY_LINE = 22
    const val LABEL_SP = 13
    const val LABEL_LINE = 18
    const val CAPTION_SP = 12
    const val CAPTION_LINE = 16

    /** Group headers are all-caps-ish, so they open up (0.06em at 12sp). */
    const val GROUP_LETTER_SPACING_EM = 0.06f
}
