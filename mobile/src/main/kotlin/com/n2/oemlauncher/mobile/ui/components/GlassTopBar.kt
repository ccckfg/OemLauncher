package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.MotionTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme

/**
 * Tracks whether any scrolling child has moved away from the top, so the bar can turn from
 * transparent into glass (design section 7). It only counts what children already consumed,
 * and it is reset on navigation so a new page starts transparent.
 */
@Stable
class TopBarScrollState(private val thresholdPx: Float) {
    var scrolled by mutableStateOf(false)
        private set

    private var offset = 0f

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (consumed.y != 0f) update(offset + consumed.y)
            return Offset.Zero
        }

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Back at the top the child stops consuming, so the bar has to follow these deltas.
            if (available.y < 0f && offset > 0f) { update(offset + available.y) }
            return Offset.Zero
        }
    }

    private fun update(value: Float) {
        offset = value.coerceIn(0f, MAX_OFFSET)
        scrolled = offset > thresholdPx
    }

    fun reset() {
        offset = 0f
        scrolled = false
    }

    private companion object {
        const val MAX_OFFSET = 100_000f
    }
}

@Composable
fun rememberTopBarScrollState(): TopBarScrollState {
    val threshold = with(LocalDensity.current) { SCROLL_THRESHOLD_DP.dp.toPx() }
    return remember { TopBarScrollState(threshold) }
}

/** Attach to the container that holds the scrollable page, above it in the tree. */
fun Modifier.trackTopBarScroll(state: TopBarScrollState): Modifier = nestedScroll(state.connection)

/** Transparent over the backdrop; glassStrong once content rolls underneath. No live blur. */
@Composable
fun GlassTopBar(
    title: String,
    scrolled: Boolean,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val palette = GlassTheme.palette
    val density = LocalDensity.current
    val background by animateColorAsState(
        targetValue = if (scrolled) palette.strongOverBackground else Color.Transparent,
        animationSpec = tween(GlassTokens.MOTION_SHORT),
        label = "topBar",
    )
    Column(modifier.fillMaxWidth().background(background)) {
        Row(
            // Same side margin as the page content, so the title lines up with the cards below.
            Modifier.fillMaxWidth().statusBarsPadding()
                .height(GlassTokens.TOP_BAR_HEIGHT.dp).padding(horizontal = UiDimensions.PAGE_PADDING.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            // The title travels with the page: in from the side it comes from, out the other.
            AnimatedContent(
                title, Modifier.weight(1f),
                transitionSpec = {
                    val shift = with(density) { MotionTokens.TITLE_SHIFT_DP.dp.roundToPx() }
                    (fadeIn(tween(MotionTokens.PAGE_FADE_IN_MS)) + slideInHorizontally { shift })
                        .togetherWith(fadeOut(tween(MotionTokens.PAGE_FADE_OUT_MS)) + slideOutHorizontally { -shift })
                },
                label = "title",
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            actions()
        }
    }
}

private const val SCROLL_THRESHOLD_DP = 8
