package com.n2.oemlauncher.wear.catalog

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Process
import android.os.UserHandle
import androidx.core.graphics.scale
import com.n2.oemlauncher.shared.design.MochaIcons
import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchCatalog
import com.n2.oemlauncher.shared.model.WatchDisplay
import com.n2.oemlauncher.shared.protocol.IconId
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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
    private val defaultIconIds = ConcurrentHashMap<String, String>()

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
            // Blocking work ignores cancellation; without this a superseded refresh keeps
            // rendering icons alongside the one that replaced it.
            ensureActive()
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
        val style = config.layout.iconStyle
        val overrideId = config.iconOverrides[entry.key]
        if (overrideId != null) {
            val cacheKey = IconCacheKey(overrideId, targetPx, shape.name, style.name)
            memory.get(cacheKey)?.let { return@withContext it }
            val bytes = customIcons.bytes(overrideId)
            val bitmap = bytes?.let { IconBitmapRenderer.decode(it, targetPx, shape) }
            if (iconRenderMode(style, false, bitmap != null) == IconRenderMode.OVERRIDE) {
                bitmap?.let {
                    memory.put(cacheKey, it)
                    return@withContext bitmap
                }
            }
        }
        val themedKey = IconCacheKey(entry.key, targetPx, shape.name, style.name)
        if (style == IconStyle.THEMED) memory.get(themedKey)?.let { return@withContext it }
        val activity by lazy {
            AppKey.parse(entry.key)?.let { parsed ->
                val component = ComponentName(parsed.packageName, parsed.className)
                launcherApps.getActivityList(component.packageName, Process.myUserHandle())
                    .firstOrNull { it.componentName == component }
            }
        }
        val drawable by lazy { activity?.getIcon(0) }
        if (style == IconStyle.THEMED) {
            val pack = AppKey.parse(entry.key)?.packageName?.let(MochaIcons::forPackage)
            // Short-circuit: a pack app never loads its own drawable.
            val monochrome = pack == null && IconBitmapRenderer.hasMonochrome(drawable)
            val themed = when (iconRenderMode(style, monochrome, inPack = pack != null)) {
                IconRenderMode.PACK -> pack?.let { MochaIconRenderer.render(it, targetPx, shape) }
                IconRenderMode.THEMED -> drawable?.let { IconBitmapRenderer.themed(it, targetPx, shape) }
                else -> null
            }
            themed?.let {
                memory.put(themedKey, it)
                return@withContext it
            }
        }
        val defaultId = entry.iconId
        if (defaultId != null) {
            val cacheKey = IconCacheKey(defaultId, targetPx, shape.name, config.layout.iconStyle.name)
            memory.get(cacheKey)?.let { return@withContext it }
            val bytes = iconStore.get(defaultId)
            if (bytes != null) IconBitmapRenderer.decode(bytes, targetPx, shape)
                ?.also { memory.put(cacheKey, it) }?.let { return@withContext it }
        }
        val generated = IconBitmapRenderer.render(drawable ?: return@withContext null, WearConstants.ICON_TARGET_PX, shape)
        val encoded = ByteArrayOutputStream().also { generated.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        iconStore.put(IconId.of(encoded), encoded)
        val display = if (generated.width == targetPx && generated.height == targetPx) generated
        else generated.scale(targetPx, targetPx)
        memory.put(IconCacheKey(IconId.of(encoded), targetPx, shape.name, config.layout.iconStyle.name), display)
        display
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

    /**
     * Rendering and PNG-encoding every icon on each launcher show took seconds on the watch. An
     * update installs the APK under a new directory, so the source path changes exactly when the
     * icon can, and the id can be reused until then.
     */
    private fun defaultIconId(activity: LauncherActivityInfo, shape: IconShape): String? {
        val memoKey = "${activity.componentName.flattenToString()}|${activity.applicationInfo.sourceDir}|$shape"
        defaultIconIds[memoKey]?.let { return it }
        return try {
            val bitmap = IconBitmapRenderer.render(activity.getIcon(0), WearConstants.ICON_TARGET_PX, shape)
            val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            iconStore.put(IconId.of(bytes), bytes)
            IconId.of(bytes).also { defaultIconIds[memoKey] = it }
        } catch (_: RuntimeException) {
            null
        }
    }

}
