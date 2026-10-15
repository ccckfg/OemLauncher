package com.n2.oemlauncher.mobile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.DemoConstants
import com.n2.oemlauncher.mobile.iconpack.IconProcessor
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DemoCatalogFactory(
    private val context: Context,
    private val icons: IconStore,
    private val processor: IconProcessor,
) {
    /** Demo labels come from resources, so they go stale when the app language changes. */
    fun matchesLanguage(catalog: WatchCatalog): Boolean =
        catalog.entries.map { it.label } ==
            context.resources.getStringArray(R.array.demo_apps).take(DemoConstants.SIZE)

    suspend fun create(): Pair<WatchCatalog, LauncherConfig> = withContext(Dispatchers.IO) {
        val labels = context.resources.getStringArray(R.array.demo_apps)
        val entries = labels.take(DemoConstants.SIZE).mapIndexed { index, label ->
            val side = ProtocolLimits.CATALOG_ICON_PX
            val bitmap = createBitmap(side, side)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.HSVToColor(
                floatArrayOf(
                    (index * DemoConstants.PALETTE_SEED % DemoConstants.PALETTE_STEPS).toFloat(),
                    DemoConstants.PALETTE_SATURATION, DemoConstants.PALETTE_VALUE,
                ),
            )
            canvas.drawColor(paint.color)
            paint.color = Color.WHITE
            paint.textSize = side * DemoConstants.ICON_TEXT_RATIO
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                label.take(1), side / 2f,
                side / 2f - (paint.ascent() + paint.descent()) / 2f, paint,
            )
            val bytes = processor.encode(bitmap)
            bitmap.recycle()
            val packageName = DemoConstants.PACKAGE_PREFIX + "app$index"
            CatalogEntry(
                "$packageName/$packageName${DemoConstants.CLASS_SUFFIX}", label,
                icons.put(bytes), launchCount = DemoConstants.SIZE - index,
                lastLaunchedMillis = index.toLong(),
            )
        }
        val catalog = WatchCatalog(
            System.currentTimeMillis(), context.getString(R.string.demo_watch),
            DemoConstants.SDK, DemoConstants.VERSION,
            WatchDisplay(DemoConstants.WIDTH_PX, DemoConstants.WIDTH_PX, DemoConstants.DENSITY_DPI, true),
            entries,
        )
        val config = entries.take(LauncherDefaults.MAX_FAVORITES).fold(LauncherConfig()) { current, app ->
            ConfigEditor.toggleFavorite(current, app.key).first
        }
        catalog to config
    }
}
