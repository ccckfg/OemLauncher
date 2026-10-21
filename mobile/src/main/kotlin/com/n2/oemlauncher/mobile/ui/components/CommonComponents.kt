package com.n2.oemlauncher.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.GlassTokens
import com.n2.oemlauncher.mobile.config.UiDimensions
import com.n2.oemlauncher.mobile.editor.CompanionViewModel
import com.n2.oemlauncher.mobile.ui.theme.GlassTheme
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig

/** Search on a glass capsule; the placeholder is a real string so it is translated. */
@Composable
fun SearchPill(query: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val palette = GlassTheme.palette
    val placeholder = stringResource(R.string.search)
    Row(
        modifier.fillMaxWidth().height(SEARCH_HEIGHT.dp)
            .glassSurface(corner = 0.dp, capsule = true)
            .padding(horizontal = GlassTokens.CARD_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp),
    ) {
        Icon(
            Icons.Outlined.Search, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SEARCH_ICON_DP.dp),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(palette.accentText),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Outlined.Clear, stringResource(R.string.clear_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SEARCH_ICON_DP.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onChange("") },
            )
        }
    }
}

/** App icon as a circle of glass with a hairline rim, so it looks set into the surface. */
@Composable
fun AppIcon(app: CatalogEntry, config: LauncherConfig, vm: CompanionViewModel, iconId: String? = null) {
    val id = iconId ?: config.iconOverrides[app.key] ?: app.iconId
    val images by vm.iconImages.collectAsStateWithLifecycle()
    LaunchedEffect(id) { if (id != null) vm.prefetchIcons(listOf(id)) }
    val image = id?.let { images[it] }
    val palette = GlassTheme.palette
    Box(
        Modifier.size(UiDimensions.APP_ICON.dp).clip(CircleShape)
            .background(palette.glassOverBackground)
            .drawBehind {
                val rim = 1.dp.toPx()
                drawCircle(palette.rimLight, radius = size.minDimension / 2f - rim / 2f, style = Stroke(rim))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, contentDescription = null, modifier = Modifier.size(UiDimensions.APP_ICON.dp).clip(CircleShape))
        } else {
            Text(app.label.take(1), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun Processing() {
    Column(
        Modifier.fillMaxWidth().padding(UiDimensions.PAGE_PADDING.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
    ) {
        CircularProgressIndicator(color = GlassTheme.palette.accentText)
        Text(
            stringResource(R.string.processing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A glass row with an icon, a title and an optional detail line; used by every list page. */
@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevated: Boolean = false,
    contentPadding: Dp = UiDimensions.SMALL_GAP.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().height(GlassTokens.ROW_HEIGHT.dp)
            .glassSurface(GlassTokens.ROW_CORNER.dp, elevated = elevated, refract = false)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiDimensions.GAP.dp),
        content = content,
    )
}

private const val SEARCH_HEIGHT = 48
private const val SEARCH_ICON_DP = 20
