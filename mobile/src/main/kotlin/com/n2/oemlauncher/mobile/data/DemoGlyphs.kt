package com.n2.oemlauncher.mobile.data

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PhonelinkRing
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import com.n2.oemlauncher.mobile.config.DemoConstants

/**
 * The demo watch's apps, drawn the way the mocha icon set will look: a cream line glyph on a
 * muted earth-tone disc. Entries follow `R.array.demo_apps`; the second value indexes
 * [DemoConstants.TONES] and is picked by meaning (health is terracotta, water is slate...).
 */
internal object DemoGlyphs {
    private val glyphs: List<Pair<ImageVector, Int>> = listOf(
        Icons.Outlined.Call to 1, Icons.Outlined.Person to 2, Icons.Outlined.ChatBubbleOutline to 5,
        Icons.Outlined.WbSunny to 0, Icons.Outlined.CalendarMonth to 3, Icons.Outlined.FavoriteBorder to 3,
        Icons.Outlined.MusicNote to 4, Icons.Outlined.Settings to 6, Icons.Outlined.Alarm to 2,
        Icons.Outlined.HourglassEmpty to 5, Icons.Outlined.Timer to 2, Icons.Outlined.Map to 5,
        Icons.Outlined.AccountBalanceWallet to 0, Icons.Outlined.PhotoCamera to 6, Icons.Outlined.Calculate to 1,
        Icons.Outlined.Edit to 2, Icons.Outlined.Mic to 3, Icons.Outlined.Explore to 6,
        Icons.Outlined.FlashlightOn to 2, Icons.Outlined.PhonelinkRing to 0,
        Icons.AutoMirrored.Outlined.DirectionsRun to 1, Icons.AutoMirrored.Outlined.DirectionsBike to 5,
        Icons.Outlined.Pool to 0, Icons.Outlined.Bedtime to 4, Icons.Outlined.MonitorHeart to 3,
        Icons.Outlined.Air to 5, Icons.AutoMirrored.Outlined.DirectionsWalk to 1, Icons.Outlined.FitnessCenter to 3,
        Icons.Outlined.Podcasts to 4, Icons.Outlined.Radio to 6, Icons.Outlined.PhotoLibrary to 4,
        Icons.Outlined.Email to 0, Icons.Outlined.TaskAlt to 2, Icons.Outlined.Translate to 5,
        Icons.Outlined.Newspaper to 6, Icons.AutoMirrored.Outlined.MenuBook to 3, Icons.Outlined.Flight to 0,
        Icons.Outlined.Home to 1, Icons.Outlined.SportsEsports to 4, Icons.Outlined.ShoppingBag to 3,
    )

    /** Paints app [index] onto a square [canvas] of [side] px; the watch masks it to its shape. */
    fun draw(canvas: Canvas, side: Int, index: Int) {
        val (glyph, tone) = glyphs[index % glyphs.size]
        val tones = DemoConstants.TONES
        canvas.drawColor(tones[tone % tones.size].toInt())
        // A little light on the upper half, so the disc reads as glazed rather than printed.
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, side * DemoConstants.SHEEN_FRACTION,
                DemoConstants.SHEEN_COLOR.toInt(), 0, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, side.toFloat(), side.toFloat(), sheen)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DemoConstants.INK.toInt() }
        val scale = side * DemoConstants.GLYPH_RATIO / glyph.viewportWidth
        val inset = (side - glyph.viewportWidth * scale) / 2f
        val matrix = Matrix().apply { setScale(scale, scale); postTranslate(inset, inset) }
        paths(glyph.root).forEach { path ->
            path.transform(matrix)
            canvas.drawPath(path, ink)
        }
    }

    private fun paths(group: VectorGroup): List<Path> = group.flatMap { node ->
        when (node) {
            is VectorPath -> listOf(node.pathData.toPath().apply { fillType = node.pathFillType }.asAndroidPath())
            is VectorGroup -> paths(node)
        }
    }
}
