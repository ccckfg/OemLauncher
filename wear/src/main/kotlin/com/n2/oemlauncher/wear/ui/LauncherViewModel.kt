package com.n2.oemlauncher.wear.ui

import android.app.Application
import android.graphics.Bitmap
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
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as App).container
    private val mutable = MutableStateFlow(LauncherUiState())
    val state: StateFlow<LauncherUiState> = mutable.asStateFlow()

    init {
        container.catalogRepository.snapshot()?.let { snapshot -> mutable.value = mutable.value.copy(entries = snapshot.entries, loading = false) }
        viewModelScope.launch {
            container.configRepository.config.collect { config ->
                mutable.value = mutable.value.copy(config = config)
                refresh(config)
            }
        }
    }

    fun refresh(config: LauncherConfig = state.value.config) {
        viewModelScope.launch(Dispatchers.IO) {
            val usage = container.configRepository.usage.first()
            val catalog = runCatching { container.catalogRepository.refresh(config, usage) }.getOrNull()
            val entries = catalog?.entries.orEmpty().let { if (state.value.demoEnabled) withDemo(it) else it }
            val arranged = LauncherArranger.arrange(entries, config)
            val icons = buildMap {
                arranged.take(160).forEach { entry ->
                    container.catalogRepository.loadIcon(entry, config)?.let { put(entry.key, it) }
                }
            }
            mutable.value = state.value.copy(entries = arranged, icons = icons, loading = false)
        }
    }

    fun openSettings(open: Boolean) { mutable.value = state.value.copy(showSettings = open, menuKey = null) }
    fun showMenu(key: String?) { mutable.value = state.value.copy(menuKey = key) }
    fun centerHoneycomb() { mutable.value = state.value.copy(centerRequest = state.value.centerRequest + 1) }

    fun toggleDemo() {
        if (!BuildConfig.DEBUG) return
        val enabled = !state.value.demoEnabled
        mutable.value = state.value.copy(demoEnabled = enabled)
        refresh()
    }

    fun launch(entry: CatalogEntry) {
        viewModelScope.launch {
            if (container.catalogRepository.launch(entry.key)) {
                container.configRepository.recordLaunch(entry.key, System.currentTimeMillis())
                mutable.value = state.value.copy(menuKey = null)
            } else {
                refresh()
            }
        }
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
