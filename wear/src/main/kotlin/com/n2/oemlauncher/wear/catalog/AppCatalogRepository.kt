package com.n2.oemlauncher.wear.catalog

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import androidx.core.graphics.drawable.toBitmap
import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import com.n2.oemlauncher.wear.config.WearConstants
import com.n2.oemlauncher.wear.data.CatalogSnapshotStore
import com.n2.oemlauncher.wear.data.ConfigRepository
import com.n2.oemlauncher.wear.data.IconFileStore
import com.n2.oemlauncher.wear.data.UsageRecord
import com.n2.oemlauncher.wear.data.UsageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps

class AppCatalogRepository(
    private val context: Context,
    private val configRepository: ConfigRepository,
    private val snapshotStore: CatalogSnapshotStore,
    private val iconStore: IconFileStore,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val packageManager = context.packageManager
    private val memory = IconMemoryCache(WearConstants.ICON_CACHE_BYTES)
    private val users = ConcurrentHashMap<String, UserHandle>()

    fun snapshot(): WatchCatalog? = snapshotStore.read()

    suspend fun query(config: LauncherConfig, usage: UsageStats): List<LaunchableApp> =
        withContext(Dispatchers.IO) {
            val activities = launcherApps.getActivityList(null, Process.myUserHandle())
                .filter { it.applicationInfo.packageName != context.packageName }
            activities.mapNotNull { activity -> toLaunchable(activity, config, usage) }
        }

    suspend fun refresh(config: LauncherConfig, usage: UsageStats): WatchCatalog = withContext(Dispatchers.IO) {
        val activities = launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.applicationInfo.packageName != context.packageName }
        val entries = activities.mapNotNull { activity ->
            val key = activity.componentName.flattenToString()
            val record = usage[key] ?: UsageRecord()
            users[key] = activity.user
            CatalogEntry(
                key = key,
                label = activity.label?.toString().orEmpty().ifBlank { activity.componentName.packageName },
                iconId = defaultIconId(activity, config.layout.iconShape),
                isSystem = (activity.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                launchCount = record.launchCount,
                lastLaunchedMillis = record.lastLaunchedMillis,
            )
        }
        WatchCatalog(
            generatedAtMillis = System.currentTimeMillis(),
            deviceName = android.os.Build.MODEL,
            sdkInt = android.os.Build.VERSION.SDK_INT,
            appVersionName = packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
            display = WatchDisplay(
                widthPx = context.resources.displayMetrics.widthPixels,
                heightPx = context.resources.displayMetrics.heightPixels,
                densityDpi = context.resources.displayMetrics.densityDpi,
                isRound = context.resources.configuration.isScreenRound,
            ),
            entries = entries,
        ).also(snapshotStore::write)
    }

    suspend fun loadIcon(entry: CatalogEntry, config: LauncherConfig): Bitmap? = withContext(Dispatchers.IO) {
        val shape = config.layout.iconShape
        val overrideId = config.iconOverrides[entry.key]
        val iconId = overrideId ?: entry.iconId ?: return@withContext null
        val cacheKey = IconCacheKey(iconId, WearConstants.ICON_TARGET_PX, shape.name)
        memory.get(cacheKey)?.let { return@withContext it }
        val bytes = iconStore.get(iconId)
        if (bytes != null) decode(bytes, shape)?.also { memory.put(cacheKey, it) }?.let { return@withContext it }
        val component = AppKey.parse(entry.key)?.let { ComponentName(it.packageName, it.className) }
            ?: return@withContext null
        val activity = launcherApps.getActivityList(component.packageName, Process.myUserHandle())
            .firstOrNull { it.componentName == component } ?: return@withContext null
        val bitmap = drawableToBitmap(activity.getIcon(0), shape)
        val encoded = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        iconStore.put(IconId.of(encoded), encoded)
        memory.put(cacheKey, bitmap)
        bitmap
    }

    suspend fun launch(key: String, sourceBounds: RectF? = null): Boolean = withContext(Dispatchers.Main.immediate) {
        val parsed = AppKey.parse(key) ?: return@withContext false
        val user = users[key] ?: Process.myUserHandle()
        val component = ComponentName(parsed.packageName, parsed.className)
        try {
            val bounds = sourceBounds ?: RectF(0f, 0f, 1f, 1f)
            val source = android.graphics.Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
            launcherApps.startMainActivity(component, user, source, ActivityOptions.makeBasic().toBundle())
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun toLaunchable(activity: LauncherActivityInfo, config: LauncherConfig, usage: UsageStats): LaunchableApp {
        val key = activity.componentName.flattenToString()
        users[key] = activity.user
        val record = usage[key] ?: UsageRecord()
        val entry = CatalogEntry(key, activity.label?.toString().orEmpty(), defaultIconId(activity, config.layout.iconShape),
            launchCount = record.launchCount, lastLaunchedMillis = record.lastLaunchedMillis)
        return LaunchableApp(entry, activity.user, null)
    }

    private fun defaultIconId(activity: LauncherActivityInfo, shape: IconShape): String? = try {
        val bitmap = drawableToBitmap(activity.getIcon(0), shape)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        iconStore.put(IconId.of(bytes), bytes)
        IconId.of(bytes)
    } catch (_: RuntimeException) {
        null
    }

    private fun decode(bytes: ByteArray, shape: IconShape): Bitmap? = runCatching {
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let {
            val resized = if (it.width == WearConstants.ICON_TARGET_PX && it.height == WearConstants.ICON_TARGET_PX) it
            else Bitmap.createScaledBitmap(it, WearConstants.ICON_TARGET_PX, WearConstants.ICON_TARGET_PX, true)
            mask(resized, shape)
        }
    }.getOrNull()

    private fun drawableToBitmap(drawable: Drawable, shape: IconShape): Bitmap {
        val source = if (drawable is AdaptiveIconDrawable) drawable else drawable
        val bitmap = source.toBitmap(WearConstants.ICON_TARGET_PX, WearConstants.ICON_TARGET_PX, Bitmap.Config.ARGB_8888)
        return mask(bitmap, shape)
    }

    private fun mask(source: Bitmap, shape: IconShape): Bitmap {
        if (shape == IconShape.ORIGINAL) return source
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val path = Path()
        val rect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        if (shape == IconShape.CIRCLE) path.addOval(rect, Path.Direction.CW)
        else path.addRoundRect(rect, source.width * 0.22f, source.width * 0.22f, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        return result
    }
}
