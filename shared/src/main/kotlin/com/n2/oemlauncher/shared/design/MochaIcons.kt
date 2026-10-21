package com.n2.oemlauncher.shared.design

import com.n2.oemlauncher.shared.design.MochaGlyph.*
import com.n2.oemlauncher.shared.design.MochaIcons.Tone.*
import kotlin.math.max
import kotlin.math.min

/**
 * The built-in mocha icon pack (design section 6, style B): a cream line glyph on a muted
 * earth-tone disc. Tones are picked by meaning and stay low in saturation, so a screen of them
 * still reads as one mocha surface while the hue helps the eye find an app.
 */
object MochaIcons {
    /** Cream glyphs keep at least 6:1 contrast on every tone. */
    enum class Tone(val argb: Long) {
        SLATE(0xFF3E4B5E), SAGE(0xFF4C5A3A), OCHRE(0xFF75582C), TERRACOTTA(0xFF7A4434),
        PLUM(0xFF5C3D52), TEAL(0xFF2F5655), CLAY(0xFF5A4636),
    }

    data class Icon(val glyph: MochaGlyph, val tone: Tone)

    const val INK = 0xFFFFF7EE

    /** Glyph width as a share of the icon's side. */
    const val GLYPH_RATIO = 0.5f

    /** A little light over the upper part, so the disc reads as glazed rather than printed. */
    const val SHEEN_COLOR = 0x24FFF3E6
    const val SHEEN_FRACTION = 0.6f

    /** Side of the square the path data is drawn in; its y axis runs from -VIEWPORT to 0. */
    const val VIEWPORT = 960f

    /** Below this saturation an icon counts as neutral and takes [Tone.CLAY]. */
    const val NEUTRAL_SATURATION = 0.18f

    private val byPackage: Map<String, Icon> = mapOf(
        "com.google.android.apps.wearable.settings" to Icon(SETTINGS, SLATE),
        "com.samsung.android.dialer" to Icon(CALL, SAGE),
        "com.samsung.android.app.contacts" to Icon(CONTACTS, OCHRE),
        "com.samsung.android.calendar" to Icon(CALENDAR_MONTH, SAGE),
        "com.samsung.android.app.reminder" to Icon(CHECKLIST, OCHRE),
        "com.samsung.android.watch.alarm" to Icon(ALARM, TERRACOTTA),
        "com.samsung.android.watch.stopwatch" to Icon(TIMER, OCHRE),
        "com.samsung.android.watch.timer" to Icon(HOURGLASS_EMPTY, TEAL),
        "com.samsung.android.watch.worldclock" to Icon(SCHEDULE, SLATE),
        "com.samsung.android.watch.weather" to Icon(PARTLY_CLOUDY_DAY, SLATE),
        "com.samsung.android.gallery.watch" to Icon(PHOTO_LIBRARY, PLUM),
        "com.samsung.android.wearable.music" to Icon(MUSIC_NOTE, PLUM),
        "com.samsung.android.wear.voicerecorder" to Icon(MIC, TERRACOTTA),
        "com.samsung.android.wear.calculator" to Icon(CALCULATE, SAGE),
        "com.samsung.android.watch.compass" to Icon(EXPLORE, CLAY),
        "com.samsung.android.wear.shealth" to Icon(FAVORITE, TEAL),
        "com.samsung.android.shealthmonitor" to Icon(MONITOR_HEART, TERRACOTTA),
        "com.codoon.gps" to Icon(DIRECTIONS_RUN, SAGE),
        "com.samsung.android.samsungpay.gear" to Icon(ACCOUNT_BALANCE_WALLET, SLATE),
        "com.samsung.android.oneconnect" to Icon(HOME, TEAL),
        "com.samsung.android.mediacontroller" to Icon(CAST, TEAL),
        "com.samsung.android.watch.budscontroller" to Icon(HEADPHONES, CLAY),
        "com.samsung.android.watch.pptcontroller" to Icon(SLIDESHOW, OCHRE),
        "com.samsung.android.watch.findmyphone" to Icon(PHONELINK_RING, SLATE),
        "com.google.android.gms" to Icon(RING_VOLUME, CLAY),
        "com.samsung.android.apps.wearable.recent" to Icon(HISTORY, CLAY),
        "com.sec.android.app.samsungapps" to Icon(SHOPPING_BAG, PLUM),
        "com.sec.android.app.sbrowser" to Icon(LANGUAGE, PLUM),
        // Brand apps get a generic glyph for what they do: a trademark is not ours to redraw.
        "com.tencent.watchwechat" to Icon(FORUM, SAGE),
        "com.tencent.qqlite" to Icon(SENTIMENT_SATISFIED, SLATE),
        "com.eg.android.AlipayGphone" to Icon(QR_CODE_SCANNER, SLATE),
        "com.baidu.BaiduMap" to Icon(MAP, TEAL),
        "com.hyperwear.launcher" to Icon(GRID_VIEW, CLAY),
    )

    /** The pack's icon for [packageName], or null when the pack does not cover the app. */
    fun forPackage(packageName: String): Icon? = byPackage[packageName]

    /**
     * The tone nearest in spirit to [argb], for apps the pack does not draw: neutral colors take
     * clay, everything else follows its hue round the wheel.
     */
    fun toneFor(argb: Int): Tone {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val high = max(r, max(g, b))
        val low = min(r, min(g, b))
        val chroma = high - low
        if (high <= 0f || chroma / high < NEUTRAL_SATURATION) return CLAY
        val hue = when (high) {
            r -> ((g - b) / chroma + SECTORS) % SECTORS
            g -> (b - r) / chroma + 2f
            else -> (r - g) / chroma + 4f
        } * DEGREES_PER_SECTOR
        return HUE_STOPS.first { hue < it.first }.second
    }

    private const val SECTORS = 6f
    private const val DEGREES_PER_SECTOR = 60f

    /** Upper hue bound of each tone, in degrees. */
    private val HUE_STOPS = listOf(
        20f to TERRACOTTA, 50f to OCHRE, 160f to SAGE, 200f to TEAL,
        260f to SLATE, 330f to PLUM, 361f to TERRACOTTA,
    )
}
