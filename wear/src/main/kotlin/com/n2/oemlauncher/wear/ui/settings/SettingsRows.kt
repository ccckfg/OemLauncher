package com.n2.oemlauncher.wear.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/** Decorative leading icon for a settings row; the row's label already describes it. */
fun vectorIcon(icon: ImageVector): @Composable BoxScope.() -> Unit = { Icon(icon, contentDescription = null) }

@Composable
fun TransformingLazyColumnItemScope.SectionHeader(text: String, spec: TransformationSpec) {
    ListSubHeader(
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        label = { Text(text) },
    )
}

/** A row that opens something or performs an action, with its current value underneath. */
@Composable
fun TransformingLazyColumnItemScope.ActionRow(
    icon: @Composable BoxScope.() -> Unit,
    label: String,
    value: String?,
    spec: TransformationSpec,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = colors,
        icon = icon,
        secondaryLabel = value?.let { text -> { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
fun TransformingLazyColumnItemScope.ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    spec: TransformationSpec,
    onChange: (Boolean) -> Unit,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        icon = vectorIcon(icon),
        label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

/** Stepped value with -/+ buttons; the label and the current value sit above the bar. */
@Composable
fun TransformingLazyColumnItemScope.SliderRow(
    label: String,
    valueText: String,
    value: Int,
    steps: IntProgression,
    spec: TransformationSpec,
    onChange: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().transformedHeight(this, spec).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valueText,
                Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueProgression = steps,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = label },
        )
    }
}
