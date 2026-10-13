package com.n2.oemlauncher.mobile.iconpack

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.n2.oemlauncher.mobile.config.ImportLimits
import com.n2.oemlauncher.mobile.data.CompanionRepository
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.config.LauncherDefaults
import com.n2.oemlauncher.shared.model.AppKey
import com.n2.oemlauncher.shared.model.CatalogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IconMatch(val app: CatalogEntry, val iconId: String)
data class ImportPreview(val matches: List<IconMatch>, val unmatched: List<String>)
class IconOverrideLimitException : Exception()

class IconImportRepository(
    private val resolver: ContentResolver,
    private val repository: CompanionRepository,
    private val processor: IconProcessor,
) {
    suspend fun photo(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val bytes = requireNotNull(resolver.openInputStream(uri)).use {
            it.readBounded(ImportLimits.PHOTO_MAX_BYTES)
        }
        require(bytes.size <= ImportLimits.PHOTO_MAX_BYTES)
        processor.decode(bytes)
    }

    suspend fun crop(key: String, bitmap: Bitmap, transform: CropTransform, circle: Boolean) =
        withContext(Dispatchers.IO) {
            val id = repository.icons.put(processor.crop(bitmap, transform, circle))
            repository.iconsChanged()
            repository.edit {
                checkLimit(it.iconOverrides.keys + key)
                ConfigEditor.setIconOverride(it, key, id)
            }
        }

    suspend fun import(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val pack = requireNotNull(resolver.openInputStream(uri)).use { ZipArchiveReader().read(it) }
        val entries = repository.state.value.catalog?.entries.orEmpty()
        val normalized = LinkedHashMap<String, String>()
        for ((name, raw) in pack.images) normalized[name] = repository.icons.put(processor.normalize(raw))
        val used = HashSet<String>()
        val matches = entries.mapNotNull { app ->
            val componentImage = pack.filters[app.key]?.takeIf { it in normalized }
            val packageImage = AppKey.parse(app.key)?.packageName?.takeIf { it in normalized }
            val name = componentImage ?: packageImage ?: return@mapNotNull null
            used += name
            IconMatch(app, normalized.getValue(name))
        }
        repository.iconsChanged()
        ImportPreview(matches, normalized.keys.filterNot { it in used })
    }

    suspend fun apply(preview: ImportPreview) = repository.edit { config ->
        checkLimit(config.iconOverrides.keys + preview.matches.map { it.app.key })
        preview.matches.fold(config) { current, match ->
            ConfigEditor.setIconOverride(current, match.app.key, match.iconId)
        }
    }

    suspend fun reset(key: String) = repository.edit { ConfigEditor.setIconOverride(it, key, null) }
    suspend fun resetAll() = repository.edit { config ->
        config.iconOverrides.keys.fold(config) { current, key ->
            ConfigEditor.setIconOverride(current, key, null)
        }
    }
    private fun checkLimit(keys: Set<String>) {
        if (keys.size > LauncherDefaults.MAX_ICON_OVERRIDES) throw IconOverrideLimitException()
    }
}
