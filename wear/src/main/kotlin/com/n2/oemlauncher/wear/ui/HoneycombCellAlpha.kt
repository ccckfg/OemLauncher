package com.n2.oemlauncher.wear.ui

import com.n2.oemlauncher.shared.design.LabelZone

internal fun honeycombCellAlpha(
    edgeAlpha: Float,
    centerY: Float,
    scaledRadius: Float,
    screenRadius: Float,
    showLabels: Boolean,
): Float = edgeAlpha * if (showLabels) LabelZone.alpha(centerY, scaledRadius, screenRadius) else 1f
