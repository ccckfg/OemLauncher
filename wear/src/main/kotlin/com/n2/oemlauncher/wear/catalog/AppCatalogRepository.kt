package com.n2.oemlauncher.wear.catalog

import android.app.ActivityOptions
import android.annotation.TargetApi
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
import android.os.Build
import android.os.UserHandle
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.protocol.IconId
import com.n2.oemlauncher.shared.protocol.ProtocolLimits
import com.n2.oemlauncher.wear.config.WearConstants
import com.n2.oemlauncher.wear.config.WearDebugLog
import com.n2.oemlauncher.wear.data.CatalogSnapshotStore
import com.n2.oemlauncher.wear.data.ConfigRepository
import com.n2.oemlauncher.wear.data.CustomIconRepository
import com.n2.oemlauncher.wear.data.IconFileStore
import com.n2.oemlauncher.wear.data.UsageRecord
import com.n2.oemlauncher.wear.data.UsageStats
import com.n2.oemlauncher.wear.sync.CatalogSource
import com.n2.oemlauncher.shared.layout.HoneycombDefaults
import com.n2.oemlauncher.shared.design.ColorMath
import com.n2.oemlauncher.shared.design.Palettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps

class AppCatalogRepository(
    private val context: Context,
    private val configRepository: ConfigRepository,
    private val snapshotStore: CatalogSnapshotStore,
    private val iconStore: IconFileStore,
    private val customIcons: CustomIconRepository,
    private val appListPermission: AppListPermissionController,
) : CatalogSource {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val packageManager = context.packageManager
    private val memory = IconMemoryCache(WearConstants.ICON_CACHE_BYTES)
    private val users = ConcurrentHashMap<String, UserHandle>()

    override fun snapshot(): WatchCatalog? = snapshotStore.read()

    /** Full rebuild: query LauncherApps, regenerate default icons and store a fresh snapshot. */
    override suspend fun rebuild(config: LauncherConfig, usage: UsageStats): WatchCatalog = refresh(config, usage)

    suspend fun query(config: LauncherConfig, usage: UsageStats): List<LaunchableApp> =
        withContext(Dispatchers.IO) {
            val activities = launcherApps.getActivityList(null, Process.myUserHandle())
                .filter { it.applicationInfo.packageName != context.packageName }
            activities.mapNotNull { activity -> toLaunchable(activity, config, usage) }
        }

    suspend fun refresh(config: LauncherConfig, usage: UsageStats): WatchCatalog = withContext(Dispatchers.IO) {
        val permission = appListPermission.status(requestAttempted = false, shouldShowRationale = false)
        val activities = runCatching {
            launcherApps.getActivityList(null, Process.myUserHandle())
                .filter { it.applicationInfo.packageName != context.packageName }
        }.getOrElse { error ->
            WearDebugLog.d {
                "catalog refresh entries=0 permissionAvailable=${permission.available} " +
                    "permissionGranted=${permission.granted} error=${error.javaClass.simpleName}"
            }
            throw error
        }
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
        ).also {
            snapshotStore.write(it)
            WearDebugLog.d {
                "catalog refresh entries=${entries.size} permissionAvailable=${permission.available} " +
                    "permissionGranted=${permission.granted}"
            }
        }
    }

    suspend fun loadIcon(entry: CatalogEntry, config: LauncherConfig): Bitmap? = withContext(Dispatchers.IO) {
        val shape = config.layout.iconShape
        val targetPx = displayTargetPx(config)
        val overrideId = config.iconOverrides[entry.key]
        if (overrideId != null) {
            val bytes = customIcons.bytes(overrideId)
            if (bytes != null) {
                decode(bytes, targetPx, shape)?.let { bitmap ->
                    memory.put(IconCacheKey(overrideId, targetPx, shape.name, config.layout.iconStyle.name), bitmap)
                    return@withContext bitmap
                }
            }
            // Custom bytes unavailable: fall through and show the app's default icon.
        }
        val defaultId = entry.iconId
        if (defaultId != null) {
            val cacheKey = IconCacheKey(defaultId, targetPx, shape.name, config.layout.iconStyle.name)
            memory.get(cacheKey)?.let { return@withContext it }
            val bytes = iconStore.get(defaultId)
            if (bytes != null) decode(bytes, targetPx, shape)?.also { memory.put(cacheKey, it) }?.let { return@withContext it }
        }
        val component = AppKey.parse(entry.key)?.let { ComponentName(it.packageName, it.className) }
            ?: return@withContext null
        val activity = launcherApps.getActivityList(component.packageName, Process.myUserHandle())
            .firstOrNull { it.componentName == component } ?: return@withContext null
        if (config.layout.iconStyle == IconStyle.THEMED) {
            themedIconBitmap(activity.getIcon(0), targetPx)?.let { themed ->
                memory.put(IconCacheKey(entry.key, targetPx, shape.name, IconStyle.THEMED.name), themed)
                return@withContext themed
            }
        }
        val generated = drawableToBitmap(activity.getIcon(0), shape)
        val encoded = ByteArrayOutputStream().also { generated.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        iconStore.put(IconId.of(encoded), encoded)
        val display = if (generated.width == targetPx && generated.height == targetPx) generated
        else generated.scale(targetPx, targetPx)
        memory.put(IconCacheKey(IconId.of(encoded), targetPx, shape.name, config.layout.iconStyle.name), display)
        display
    }

    /** Builds a circular glass disc only when the platform supplies a monochrome adaptive layer. */
    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private fun themedIconBitmap(drawable: Drawable, targetPx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val adaptive = drawable as? AdaptiveIconDrawable ?: return null
        val monochrome = adaptive.monochrome ?: return null
        if (iconRenderMode(IconStyle.THEMED, hasMonochromeLayer = true) != IconRenderMode.THEMED) return null
        val bitmap = androidx.core.graphics.createBitmap(targetPx, targetPx)
        val canvas = Canvas(bitmap)
        val glass = ColorMath.compositeOver(Palettes.Mocha.glass, Palettes.Mocha.background).toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = glass
        canvas.drawCircle(targetPx / 2f, targetPx / 2f, targetPx / 2f, paint)
        monochrome.setTint(Palettes.Mocha.textPrimary.toInt())
        monochrome.setBounds(0, 0, targetPx, targetPx)
        monochrome.draw(canvas)
        return bitmap
    }

    /** Pixels an icon occupies in the launcher, from the current layout's icon size preset. */
    private fun displayTargetPx(config: LauncherConfig): Int {
        val dp = HoneycombDefaults.iconDiameterDp(config.layout.iconSize)
        return (dp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
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

    private fun decode(bytes: ByteArray, targetPx: Int, shape: IconShape): Bitmap? = runCatching {
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let {
            val resized = if (it.width == targetPx && it.height == targetPx) it
            else it.scale(targetPx, targetPx)
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
        val result = createBitmap(source.width, source.height)
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
