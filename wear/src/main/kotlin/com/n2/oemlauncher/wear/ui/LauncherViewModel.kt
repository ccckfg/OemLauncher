package com.n2.oemlauncher.wear.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.protocol.RevisionPolicy
import com.n2.oemlauncher.wear.BuildConfig
import com.n2.oemlauncher.wear.App
import com.n2.oemlauncher.wear.config.WearConstants
import com.n2.oemlauncher.wear.domain.LauncherArranger
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
    val loading: Boolean = true,
    val showSettings: Boolean = false,
    val menuKey: String? = null,
    val demoEnabled: Boolean = false,
    val lastSyncMillis: Long = 0,
    val centerRequest: Int = 0,
    val message: String? = null,
    val rotaryTicks: Int = 0,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as App).container
    private val mutable = MutableStateFlow(LauncherUiState())
    val state: StateFlow<LauncherUiState> = mutable.asStateFlow()
    private var active = true
    private var refreshJob: Job? = null
    private var refreshGeneration = 0L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = container.catalogRepository.snapshot()
            if (snapshot != null) withContext(Dispatchers.Main.immediate) {
                mutable.value = state.value.copy(entries = LauncherArranger.arrange(snapshot.entries, state.value.config), loading = false)
            }
        }
        viewModelScope.launch {
            container.configRepository.config.collect { config ->
                mutable.value = mutable.value.copy(config = config)
                if (active) refresh(config)
            }
        }
    }

    fun refresh(config: LauncherConfig = state.value.config) {
        if (!active) return
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            val usage = container.configRepository.usage.first()
            val catalog = runCatching { container.catalogRepository.refresh(config, usage) }.getOrNull()
            val entries = (catalog?.entries ?: state.value.entries).let { if (state.value.demoEnabled) withDemo(it) else it }
            val arranged = LauncherArranger.arrange(entries, config)
            val icons = buildMap {
                arranged.forEach { entry ->
                    val bitmap = container.catalogRepository.loadIcon(entry, config)
                        ?: entry.key.takeIf { it.startsWith("com.n2.demo.") }?.let { DemoIconFactory.create(entry.label) }
                    bitmap?.let { put(entry.key, it) }
                }
            }
            withContext(Dispatchers.Main.immediate) {
                if (generation == refreshGeneration) {
                    mutable.value = state.value.copy(entries = arranged, icons = icons, loading = false)
                }
            }
        }
    }

    fun onVisible() { active = true; refresh() }
    fun onHidden() { active = false; refreshJob?.cancel() }

    fun openSettings(open: Boolean) { mutable.value = state.value.copy(showSettings = open, menuKey = null) }
    fun showMenu(key: String?) { mutable.value = state.value.copy(menuKey = key) }
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
                container.configRepository.recordLaunch(entry.key, System.currentTimeMillis())
                mutable.value = state.value.copy(menuKey = null, message = null)
            } else {
                mutable.value = state.value.copy(message = getApplication<Application>().getString(com.n2.oemlauncher.wear.R.string.launch_failed))
                refresh()
            }
        }
    }

    fun openAppInfo(key: String) {
        val packageName = com.n2.oemlauncher.shared.model.AppKey.parse(key)?.packageName ?: return
        getApplication<Application>().startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        showMenu(null)
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
    fun setLayout(mode: LayoutMode) = edit { ConfigEditor.updateLayout(it) { layout -> layout.copy(mode = mode) } }
    fun setSortMode(mode: SortMode) = edit { ConfigEditor.setSortMode(it, mode) }
    fun updateConfig(transform: (LauncherConfig) -> LauncherConfig) = edit(transform)

    private fun edit(transform: (LauncherConfig) -> LauncherConfig) {
        viewModelScope.launch {
            val current = state.value.config
            val next = ConfigEditor.stamp(transform(current), RevisionPolicy.next(current.revision),
                com.n2.oemlauncher.shared.model.ConfigOrigin.WATCH, System.currentTimeMillis())
            container.configRepository.save(next)
            container.syncRepository.publishState(next)
            refresh(next)
        }
    }

    private fun withDemo(entries: List<CatalogEntry>): List<CatalogEntry> {
        val existing = entries.mapTo(HashSet()) { it.key }
        return entries + (0 until WearConstants.DEMO_ENTRY_COUNT).map { index ->
            val key = "com.n2.demo.app${index + 1}/com.n2.demo.app${index + 1}.DemoActivity"
            CatalogEntry(key, "Demo ${index + 1}").takeUnless { key in existing }
        }.filterNotNull()
    }
}
