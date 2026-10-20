package com.n2.oemlauncher.wear.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.ApplyResult
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.wear.BuildConfig
import com.n2.oemlauncher.wear.App
import com.n2.oemlauncher.wear.catalog.AppListPermissionStatus
import com.n2.oemlauncher.wear.catalog.shouldRetryEmptyCatalog
import com.n2.oemlauncher.wear.config.WearConstants
import com.n2.oemlauncher.wear.config.WearDebugLog
import com.n2.oemlauncher.wear.domain.CatalogSignature
import com.n2.oemlauncher.wear.domain.LauncherArranger
import com.n2.oemlauncher.wear.home.HomeSettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

data class LauncherUiState(
    val config: LauncherConfig = LauncherConfig(),
    val entries: List<CatalogEntry> = emptyList(),
    val icons: Map<String, Bitmap> = emptyMap(),
    /** Apps the user hid; they are not in [entries] but settings lists them by name. */
    val hiddenEntries: List<CatalogEntry> = emptyList(),
    val loading: Boolean = true,
    /** Bumped when every screen above the launcher home must close (Home press, leaving the app). */
    val homeRequest: Int = 0,
    val demoEnabled: Boolean = false,
    val centerRequest: Int = 0,
    val message: String? = null,
    val rotaryTicks: Int = 0,
    /** null until the first Data Layer attempt of this process; then whether it reached the phone. */
    val syncConnected: Boolean? = null,
    /** Formatted local time of the last successful send; null when nothing was sent yet. */
    val syncLastText: String? = null,
    val syncResult: ApplyResult = ApplyResult.NONE,
    val syncMissingIcons: Int = 0,
    val homeSettings: HomeSettingsState = HomeSettingsState(),
    val appListPermission: AppListPermissionStatus = AppListPermissionStatus(false, false, false, false),
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as App).container
    private val restored = lastShown
    private val mutable = MutableStateFlow(restored?.let(::reopenedState) ?: LauncherUiState())
    val state: StateFlow<LauncherUiState> = mutable.asStateFlow()
    private var active = true
    private var refreshJob: Job? = null
    private var refreshGeneration = 0L
    private val storedCatalog = if (restored != null) null else viewModelScope.launch(Dispatchers.IO) { showStoredCatalog() }
    private var lastCatalogSignature: String? = null
    private var appListPermissionRequested = false

    init {
        WearDebugLog.d { "view model created restoredIcons=${restored?.icons?.size}" }
        viewModelScope.launch { state.collect { lastShown = it } }
        viewModelScope.launch {
            container.configRepository.config.collect { config ->
                val changed = config != state.value.config
                mutable.value = mutable.value.copy(config = config)
                // A reopened launcher starts with this config and onStart already refreshed with it.
                if (active && (changed || refreshJob == null)) refresh(config)
            }
        }
        refreshPlatformStatus()
        viewModelScope.launch {
            container.syncRepository.status.collect { status ->
                mutable.value = state.value.copy(
                    syncConnected = status.connected,
                    syncLastText = if (status.lastSyncMillis > 0) formatSyncTime(status.lastSyncMillis) else null,
                    syncResult = status.lastResult,
                    syncMissingIcons = status.missingIconCount,
                )
            }
        }
    }

    fun refresh(config: LauncherConfig = state.value.config) {
        if (!active) return
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            // The watch has two cores; a full refresh next to the stored catalog would delay both.
            storedCatalog?.join()
            val usage = container.configRepository.usage.first()
            var catalog = runCatching { container.catalogRepository.refresh(config, usage) }.getOrNull()
            if (catalog != null && shouldRetryEmptyCatalog(catalog.entries.size)) {
                kotlinx.coroutines.delay(WearConstants.CATALOG_EMPTY_RETRY_DELAY_MS)
                val retry = runCatching { container.catalogRepository.refresh(config, usage) }.getOrNull()
                if (retry != null) catalog = retry
            }
            if (catalog != null) publishCatalogWhenChanged(catalog)
            val entries = (catalog?.entries ?: state.value.entries).let { if (state.value.demoEnabled) withDemo(it) else it }
            val arranged = LauncherArranger.arrange(entries, config)
            val hidden = entries.filter { it.key in config.hidden }
            val icons = loadIcons(arranged, config)
            withContext(Dispatchers.Main.immediate) {
                if (generation == refreshGeneration) {
                    WearDebugLog.d { "icons published count=${icons.size} source=refresh" }
                    mutable.value = state.value.copy(entries = arranged, hiddenEntries = hidden, icons = icons, loading = false)
                }
            }
        }
    }

    /**
     * A fresh process has no [lastShown], but the catalog snapshot and the icon files are on disk:
     * decoding those is far cheaper than the full [refresh], which re-reads every app.
     */
    private suspend fun showStoredCatalog() {
        val snapshot = container.catalogRepository.snapshot() ?: return
        val config = container.configRepository.config.first()
        val arranged = LauncherArranger.arrange(snapshot.entries, config)
        val hidden = snapshot.entries.filter { it.key in config.hidden }
        val icons = loadIcons(arranged, config)
        withContext(Dispatchers.Main.immediate) {
            WearDebugLog.d { "icons published count=${icons.size} source=snapshot" }
            mutable.value = state.value.copy(entries = arranged, hiddenEntries = hidden, icons = icons, loading = false)
        }
    }

    private suspend fun loadIcons(entries: List<CatalogEntry>, config: LauncherConfig): Map<String, Bitmap> = buildMap {
        entries.forEach { entry ->
            val bitmap = container.catalogRepository.loadIcon(entry, config)
                ?: entry.key.takeIf { it.startsWith("com.n2.demo.") }?.let { DemoIconFactory.create(entry.label) }
            bitmap?.let { put(entry.key, it) }
        }
    }

    /**
     * Package events and icon-shape changes both funnel through [refresh]; a content change is
     * what the phone needs to hear about. Publishing reads the snapshot written by this refresh,
     * so it never rebuilds the icons again.
     */
    private suspend fun publishCatalogWhenChanged(catalog: com.n2.oemlauncher.shared.model.WatchCatalog) {
        val signature = CatalogSignature.of(catalog)
        val previous = lastCatalogSignature
        lastCatalogSignature = signature
        if (previous != null && previous != signature) container.syncRepository.requestCatalogPublish()
    }

    fun onVisible() { active = true; refreshPlatformStatus(); refresh() }
    fun onHidden() { active = false; refreshJob?.cancel() }

    fun goHome() { mutable.value = state.value.copy(homeRequest = state.value.homeRequest + 1) }
    fun centerHoneycomb() { mutable.value = state.value.copy(centerRequest = state.value.centerRequest + 1) }

    fun toggleDemo() {
        if (!BuildConfig.DEBUG) return
        val enabled = !state.value.demoEnabled
        mutable.value = state.value.copy(demoEnabled = enabled)
        refresh()
    }

    fun launch(entry: CatalogEntry, sourceBounds: RectF? = null) {
        viewModelScope.launch {
            if (container.catalogRepository.launch(entry.key, sourceBounds)) {
                container.foregroundState.recordAppLaunch()
                val count = container.configRepository.recordLaunch(entry.key, System.currentTimeMillis())
                if (count % WearConstants.USAGE_PUBLISH_EVERY == 0) container.syncRepository.requestCatalogPublish()
                mutable.value = state.value.copy(message = null)
            } else {
                mutable.value = state.value.copy(message = getApplication<Application>().getString(com.n2.oemlauncher.wear.R.string.launch_failed))
                refresh()
            }
        }
    }

    fun openAppInfo(key: String) {
        val packageName = com.n2.oemlauncher.shared.model.AppKey.parse(key)?.packageName ?: return
        // No NEW_TASK: the launcher activity is on screen when this runs.
        getApplication<Application>().startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()),
        )
    }

    fun refreshPlatformStatus(shouldShowPermissionRationale: Boolean = false) {
        mutable.value = state.value.copy(
            homeSettings = container.homeSettingsRepository.read(),
            appListPermission = container.appListPermission.status(
                requestAttempted = appListPermissionRequested,
                shouldShowRationale = shouldShowPermissionRationale,
            ),
        )
    }

    fun markAppListPermissionRequested(shouldShowPermissionRationale: Boolean = false) {
        appListPermissionRequested = true
        refreshPlatformStatus(shouldShowPermissionRationale)
    }

    fun setHomeSettingsMessage(message: String?) {
        mutable.value = state.value.copy(homeSettings = state.value.homeSettings.copy(message = message))
    }

    fun moveFavorite(key: String, delta: Int) {
        edit { config ->
            val index = config.favorites.indexOf(key)
            if (index < 0) config else ConfigEditor.moveFavorite(config, index, (index + delta).coerceIn(0, config.favorites.lastIndex))
        }
    }

    fun markRotaryStep() {
        mutable.value = state.value.copy(centerRequest = state.value.centerRequest + 1)
    }

    fun onRotary(delta: Float) {
        val steps = delta.toInt().coerceIn(-32, 32)
        if (steps != 0) mutable.value = state.value.copy(rotaryTicks = state.value.rotaryTicks + steps)
    }

    fun toggleFavorite(key: String) = edit { ConfigEditor.toggleFavorite(it, key).first }
    fun toggleHidden(key: String) = edit { ConfigEditor.setHidden(it, key, true) }
    fun unhide(key: String) = edit { ConfigEditor.setHidden(it, key, false) }
    fun setLayout(mode: LayoutMode) = edit { ConfigEditor.updateLayout(it) { layout -> layout.copy(mode = mode) } }
    fun setSortMode(mode: SortMode) = edit { ConfigEditor.setSortMode(it, mode) }
    fun updateConfig(transform: (LauncherConfig) -> LauncherConfig) = edit(transform)

    private fun edit(transform: (LauncherConfig) -> LauncherConfig) {
        viewModelScope.launch {
            // One transaction with the sync repository: revision, persistence and the state
            // receipt can no longer race with an incoming desired config.
            val next = container.syncRepository.localEdit(transform)
            refresh(next)
        }
    }

    /** Local-time label for the last successful sync, e.g. "14:32" or "10/15 14:32". */
    private fun formatSyncTime(millis: Long): String {
        val context = getApplication<Application>()
        val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(millis))
        val day = android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(millis))
        val today = android.text.format.DateFormat.getDateFormat(context).format(java.util.Date())
        return if (day == today) time else "$day $time"
    }

    private companion object {
        /**
         * Every exit finishes the launcher activity, so each Home press builds a new view model,
         * while the process usually lives on. Starting from what the honeycomb last showed puts the
         * icons in the very first frame; [refresh] then only applies what changed. Main thread only.
         */
        var lastShown: LauncherUiState? = null

        fun reopenedState(last: LauncherUiState) = LauncherUiState(
            config = last.config,
            entries = last.entries,
            icons = last.icons,
            hiddenEntries = last.hiddenEntries,
            loading = last.loading,
        )
    }

    private fun withDemo(entries: List<CatalogEntry>): List<CatalogEntry> {
        val existing = entries.mapTo(HashSet()) { it.key }
        return entries + (0 until WearConstants.DEMO_ENTRY_COUNT).map { index ->
            val key = "com.n2.demo.app${index + 1}/com.n2.demo.app${index + 1}.DemoActivity"
            CatalogEntry(key, "Demo ${index + 1}").takeUnless { key in existing }
        }.filterNotNull()
    }
}
