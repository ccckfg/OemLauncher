package com.n2.oemlauncher.mobile.data

import android.graphics.Canvas
import com.n2.oemlauncher.mobile.iconpack.MochaIconArt
import com.n2.oemlauncher.shared.design.MochaGlyph.*
import com.n2.oemlauncher.shared.design.MochaIcons
import com.n2.oemlauncher.shared.design.MochaIcons.Tone.*

/**
 * The demo watch's apps, drawn in the watch's mocha icon pack. Entries follow
 * `R.array.demo_apps`; tones are picked by meaning (health is terracotta, water is slate...).
 */
internal object DemoGlyphs {
    private val icons: List<MochaIcons.Icon> = listOf(
        CALL to SAGE, CONTACTS to OCHRE, CHAT_BUBBLE to TEAL, PARTLY_CLOUDY_DAY to SLATE,
        CALENDAR_MONTH to TERRACOTTA, FAVORITE to TERRACOTTA, MUSIC_NOTE to PLUM, SETTINGS to CLAY,
        ALARM to OCHRE, HOURGLASS_EMPTY to TEAL, TIMER to OCHRE, MAP to TEAL,
        ACCOUNT_BALANCE_WALLET to SLATE, PHOTO_CAMERA to CLAY, CALCULATE to SAGE, EDIT to OCHRE,
        MIC to TERRACOTTA, EXPLORE to CLAY, FLASHLIGHT_ON to OCHRE, PHONELINK_RING to SLATE,
        DIRECTIONS_RUN to SAGE, DIRECTIONS_BIKE to TEAL, POOL to SLATE, BEDTIME to PLUM,
        MONITOR_HEART to TERRACOTTA, AIR to TEAL, DIRECTIONS_WALK to SAGE, FITNESS_CENTER to TERRACOTTA,
        PODCASTS to PLUM, RADIO to CLAY, PHOTO_LIBRARY to PLUM, MAIL to SLATE,
        TASK_ALT to OCHRE, TRANSLATE to TEAL, NEWSPAPER to CLAY, MENU_BOOK to TERRACOTTA,
        FLIGHT to SLATE, HOME to SAGE, SPORTS_ESPORTS to PLUM, SHOPPING_BAG to TERRACOTTA,
    ).map { (glyph, tone) -> MochaIcons.Icon(glyph, tone) }

    /** Paints app [index] onto a square [canvas] of [side] px; the watch masks it to its shape. */
    fun draw(canvas: Canvas, side: Int, index: Int) = MochaIconArt.draw(canvas, side, icons[index % icons.size])
}
