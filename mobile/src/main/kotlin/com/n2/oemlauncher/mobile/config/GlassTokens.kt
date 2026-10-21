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
    const val SHEET_CORNER = 34
    /** Sheets float clear of the screen edges, as one more pane of glass. */
    const val SHEET_MARGIN = 10

    const val TOP_BAR_HEIGHT = 48
    const val CARD_PADDING = 16
    const val CHIP_HEIGHT = 34
    const val CHIP_PADDING = 14
    const val ROW_HEIGHT = 64
    const val SETTING_ROW = 52
    const val SEGMENT_HEIGHT = 46
    const val SEGMENT_PADDING = 4
    const val TILE_HEIGHT = 132
    const val TILE_ICON = 40
    const val BUTTON_HEIGHT = 48
    const val STATUS_DOT = 8

    /** Switch track and its pill knob; the knob is wider than tall, like a drop. */
    const val SWITCH_WIDTH = 60
    const val SWITCH_HEIGHT = 30
    const val SWITCH_KNOB_WIDTH = 36
    const val SWITCH_KNOB_HEIGHT = 24
    const val SWITCH_INSET = 3
    const val SLIDER_TRACK = 10
    const val SLIDER_KNOB_WIDTH = 40
    const val SLIDER_KNOB_HEIGHT = 26

    /** Knobs: held, they swell by [KNOB_PRESSED_SCALE] and clear into a lens. */
    const val KNOB_PRESSED_SCALE = 1.35f
    const val KNOB_LENS_EDGE = 8f
    const val KNOB_LENS_AMOUNT = 12f
    const val KNOB_CLEARING = 0.85f
    const val KNOB_SPECULAR = 0.7f
    const val KNOB_SHADOW = 6
    const val KNOB_SHADOW_ALPHA = 0.3f
    const val KNOB_INNER_SHADOW = 4
    /** A segment pill is wide, so it swells less than a round knob. */
    const val PILL_PRESSED_SCALE = 1.1f

    /** Specular rim: alpha kept between the lobes, where the lobes end, the far lobe's share. */
    const val RIM_FAINT = 0.12f
    const val RIM_LOBE = 0.32f
    const val RIM_BACK_LOBE = 0.7f

    /** Recessed tracks: the inner shadow along their upper edge. */
    const val RECESS_SHADOW = 4
    const val RECESS_ALPHA = 0.35f

    /** Drag lifts a row: shadow plus a small scale, like a card picked off the table. */
    const val DRAG_SCALE = 1.02f

    /**
     * Liquid glass (the backdrop library). The lens bends what lies within [LENS_EDGE] of the
     * rim by [LENS_STRENGTH] times that distance; capsules are small, so their band is
     * narrower. The tint fades towards the top, where the light comes through.
     */
    const val LENS_EDGE = 20f
    const val LENS_EDGE_CAPSULE = 12f
    const val LENS_STRENGTH = 1.8f
    const val LIQUID_TINT_TOP = 0.5f
    const val LIQUID_TINT_BOTTOM = 0.85f
    const val HIGHLIGHT_WIDTH = 1.25f

    /** The rim light is a quiet 22% on Mocha; the specular edge needs about twice that. */
    const val SPECULAR_GAIN = 2.2f
    const val SHADOW_RADIUS = 18
    const val CHAMPAGNE_SPECULAR = 0.6f
    const val CHAMPAGNE_OPACITY = 0.94f

    /** Hero preview on the home page and the smaller one on the appearance page. */
    const val HERO_PREVIEW = 236
    const val SMALL_PREVIEW = 168
    const val PREVIEW_MAX = 280
    const val PREVIEW_MIN = 216
    const val PREVIEW_FOOTER = 140
    const val BEZEL_RING = 2
    const val PREVIEW_BORDER = 6

    /** Scrim behind a dialog or bottom sheet when the window cannot blur what is behind it. */
    const val SCRIM_ALPHA = 0.3f
    /** Overlays keep this share of the strong glass tint over the blurred page. */
    const val OVERLAY_TINT = 0.75f

    /** Ambient glows of the page background, as fractions of the page size. */
    const val WARM_GLOW_X = 0.12f
    const val WARM_GLOW_Y = 0.06f
    const val WARM_GLOW_RADIUS = 0.47f
    const val COOL_GLOW_X = 0.92f
    const val COOL_GLOW_Y = 0.84f
    const val COOL_GLOW_RADIUS = 0.42f

    /**
     * Soft pools of light on the page, as fractions of the page (radius of its height). Glass
     * refracts their edges; without them the page is a smooth gradient and the lens shows nothing.
     */
    val LIGHT_POOLS = listOf(
        LightPool(x = 0.16f, y = 0.30f, radius = 0.16f, alpha = 0.22f),
        LightPool(x = 0.88f, y = 0.14f, radius = 0.10f, alpha = 0.18f),
        LightPool(x = 0.08f, y = 0.74f, radius = 0.15f, alpha = 0.8f, cool = true),
        LightPool(x = 0.78f, y = 0.56f, radius = 0.12f, alpha = 0.16f),
    )
    const val POOL_PLATEAU = 0.55f

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

/** A pool of light on the page backdrop: champagne, or the cool glow when [cool]. */
data class LightPool(val x: Float, val y: Float, val radius: Float, val alpha: Float, val cool: Boolean = false)

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
