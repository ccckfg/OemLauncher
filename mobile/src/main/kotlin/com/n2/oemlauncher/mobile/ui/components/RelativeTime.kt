package com.n2.oemlauncher.mobile.ui.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.n2.oemlauncher.mobile.R

/** A relative label is either a plain string or a plural plus its quantity. */
sealed interface RelativeLabel {
    data class Plain(@StringRes val text: Int) : RelativeLabel
    data class Plural(@PluralsRes val text: Int, val count: Int) : RelativeLabel
}

/** Turns past timestamps into "2 minutes ago" style labels, in whatever locale is active. */
object RelativeTime {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS

    /** Null when nothing was recorded yet; future or sub-minute times read "just now". */
    fun label(millis: Long, now: Long): RelativeLabel? {
        if (millis <= 0L) return null
        val elapsed = now - millis
        return when {
            elapsed < MINUTE_MS -> RelativeLabel.Plain(R.string.time_just_now)
            elapsed < HOUR_MS ->
                RelativeLabel.Plural(R.plurals.time_minutes_ago, (elapsed / MINUTE_MS).toInt())
            elapsed < DAY_MS ->
                RelativeLabel.Plural(R.plurals.time_hours_ago, (elapsed / HOUR_MS).toInt())
            else -> RelativeLabel.Plural(R.plurals.time_days_ago, (elapsed / DAY_MS).toInt())
        }
    }
}

@Composable
fun relativeTime(millis: Long): String = when (val label = RelativeTime.label(millis, System.currentTimeMillis())) {
    null -> stringResource(R.string.diag_never)
    is RelativeLabel.Plain -> stringResource(label.text)
    is RelativeLabel.Plural -> pluralStringResource(label.text, label.count, label.count)
}
