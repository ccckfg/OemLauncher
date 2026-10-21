package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import kotlin.math.roundToInt

/** One visible honeycomb cell exposed to accessibility, at the settled center it draws at. */
class A11yNode(val cell: Int, val centerX: Float, val centerY: Float, val diameter: Float)

/**
 * Invisible per-cell nodes so TalkBack can reach the gear and every visible icon individually.
 * They carry semantics actions only, never pointer input, so drags and taps still reach the
 * canvas; and they sit above the canvas because Compose drops semantics nodes fully covered by
 * a later sibling.
 */
@Composable
fun HoneycombA11yOverlay(
    nodes: List<A11yNode>,
    labelOf: (Int) -> String,
    actionLabelOf: (Int) -> String,
    onActivate: (Int) -> Unit,
    onAction: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val minDiameter = with(density) { HoneycombDefaults.MIN_TOUCH_DIAMETER_DP.dp.toPx() }
    nodes.forEach { node ->
        val label = labelOf(node.cell)
        if (label.isEmpty()) return@forEach
        key(node.cell) {
            val diameter = node.diameter.coerceAtLeast(minDiameter)
            val x = (node.centerX - diameter / 2f).roundToInt()
            val y = (node.centerY - diameter / 2f).roundToInt()
            val actionLabel = actionLabelOf(node.cell)
            Box(
                Modifier
                    .offset { IntOffset(x, y) }
                    .size(with(density) { diameter.toDp() })
                    .semantics {
                        contentDescription = label
                        onClick(label = label) {
                            onActivate(node.cell)
                            true
                        }
                        customActions = listOf(
                            CustomAccessibilityAction(actionLabel) {
                                onAction(node.cell)
                                true
                            },
                        )
                    },
            )
        }
    }
}
