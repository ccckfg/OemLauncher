package com.n2.oemlauncher.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.graphics.createBitmap
import com.n2.oemlauncher.mobile.iconpack.MochaIconArt
import com.n2.oemlauncher.shared.design.MochaIcons
import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The mocha icons the watch will draw for [apps], keyed by app, so the preview matches it.
 * Empty unless the style is mocha; apps with a custom icon keep it, as on the watch. Painted
 * off the main thread, once per set of apps.
 */
@Composable
fun rememberMochaIcons(apps: List<CatalogEntry>, config: LauncherConfig): Map<String, Bitmap> {
    val keys = if (config.layout.iconStyle == IconStyle.THEMED) {
        apps.map { it.key }.filter { it !in config.iconOverrides }
    } else {
        emptyList()
    }
    val icons by produceState(emptyMap<String, Bitmap>(), keys) {
        value = withContext(Dispatchers.Default) {
            val side = ProtocolLimits.CATALOG_ICON_PX
            keys.mapNotNull { key ->
                val icon = AppKey.parse(key)?.packageName?.let(MochaIcons::forPackage) ?: return@mapNotNull null
                key to createBitmap(side, side).also { MochaIconArt.draw(Canvas(it), side, icon) }
            }.toMap()
        }
    }
    return icons
}
