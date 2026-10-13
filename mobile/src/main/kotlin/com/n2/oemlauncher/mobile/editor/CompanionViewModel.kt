package com.n2.oemlauncher.mobile.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.n2.oemlauncher.mobile.AppContainer
import com.n2.oemlauncher.mobile.data.UserNotice
import com.n2.oemlauncher.mobile.data.ConnectionKind
import com.n2.oemlauncher.mobile.data.WatchConnection
import com.n2.oemlauncher.mobile.iconpack.CropTransform
import com.n2.oemlauncher.mobile.iconpack.ImportPreview
import com.n2.oemlauncher.mobile.iconpack.IconOverrideLimitException
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.LayoutSettings
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.ordering.AppOrdering
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportState(
    val photo: Bitmap? = null,
    val preview: ImportPreview? = null,
    val busy: Boolean = false,
)

class CompanionViewModel(val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    val state = repository.state
    private val mutableImport = MutableStateFlow(ImportState())
    val importState = mutableImport.asStateFlow()
    private var discovery: Job? = null
    private var operation: Job? = null
    private var foreground = false

    fun start() {
        foreground = true
        container.gateway.observe { if (foreground) refresh() }
        refresh()
    }
    fun stop() {
        foreground = false
        discovery?.cancel()
        container.gateway.stopObserving()
    }
    fun refresh() {
        discovery?.cancel()
        discovery = viewModelScope.launch {
            try {
                val connection = container.gateway.discover()
                repository.connection(connection)
                if (connection.kind == ConnectionKind.PLAY_SERVICES_MISSING) return@launch
                container.gateway.readExisting()
                container.gateway.requestCatalog()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                repository.connection(WatchConnection())
            }
        }
    }
    fun demo() = launch {
        val (catalog, config) = container.demo.create()
        repository.demo(catalog, config)
        repository.iconsChanged()
    }
    fun favorite(key: String) = launch { repository.toggleFavorite(key) }
    fun hide(key: String, hidden: Boolean) = launch {
        repository.edit { ConfigEditor.setHidden(it, key, hidden) }
    }
    fun sort(mode: SortMode) = launch { repository.edit { ConfigEditor.setSortMode(it, mode) } }
    fun layout(transform: (LayoutSettings) -> LayoutSettings) = launch {
        repository.edit { ConfigEditor.updateLayout(it, transform) }
    }
    fun moveFavorite(from: Int, to: Int) = launch {
        repository.edit { ConfigEditor.moveFavorite(it, from, to) }
    }
    fun moveFavoriteKey(key: String, delta: Int) = launch {
        repository.edit {
            val from = it.favorites.indexOf(key)
            ConfigEditor.moveFavorite(it, from, from + delta)
        }
    }
    fun moveApp(key: String, delta: Int) = launch {
        repository.edit { config ->
            val order = AppOrdering.arrange(state.value.catalog?.entries.orEmpty(), config).others.map { it.key }
            val from = order.indexOf(key)
            ConfigEditor.moveApp(config, order, from, (from + delta).coerceIn(0, (order.size - 1).coerceAtLeast(0)))
        }
    }
    fun undo() = launch { repository.undo() }
    fun redo() = launch { repository.redo() }
    fun sync(overwrite: Boolean = false) = launch { repository.sync(container.gateway, overwrite) }
    fun loadWatch() = launch { repository.loadWatch() }
    fun dismissNotice() = repository.notice(UserNotice.NONE)
    fun resetIcon(key: String) = launch { container.importer.reset(key) }
    fun resetIcons() = launch { container.importer.resetAll() }

    fun photo(uri: Uri) {
        operation?.cancel()
        operation = launch {
            mutableImport.value = ImportState(busy = true)
            try {
                mutableImport.value = ImportState(photo = container.importer.photo(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableImport.value = ImportState()
                repository.notice(UserNotice.ICON_FAILED)
            }
        }
    }
    fun crop(key: String, transform: CropTransform, circle: Boolean, done: () -> Unit) {
        val bitmap = mutableImport.value.photo ?: return
        operation = launch {
            mutableImport.value = mutableImport.value.copy(busy = true)
            try {
                container.importer.crop(key, bitmap, transform, circle)
                mutableImport.value = ImportState()
                done()
            } catch (_: IconOverrideLimitException) {
                mutableImport.value = mutableImport.value.copy(busy = false)
                repository.notice(UserNotice.ICON_LIMIT)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableImport.value = mutableImport.value.copy(busy = false)
                repository.notice(UserNotice.ICON_FAILED)
            }
        }
    }
    fun importZip(uri: Uri) {
        operation?.cancel()
        operation = launch {
            mutableImport.value = ImportState(busy = true)
            try {
                mutableImport.value = ImportState(preview = container.importer.import(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableImport.value = ImportState()
                repository.notice(UserNotice.IMPORT_FAILED)
            }
        }
    }
    fun applyImport(done: () -> Unit) = launch {
        try {
            mutableImport.value.preview?.let { container.importer.apply(it) }
            mutableImport.value = ImportState()
            done()
        } catch (_: IconOverrideLimitException) {
            repository.notice(UserNotice.ICON_LIMIT)
        }
    }
    fun cancelImport() {
        operation?.cancel()
        mutableImport.value = ImportState()
    }
    private fun launch(block: suspend () -> Unit): Job = viewModelScope.launch { block() }
    override fun onCleared() { stop() }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CompanionViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return CompanionViewModel(container) as T
        }
    }
}
