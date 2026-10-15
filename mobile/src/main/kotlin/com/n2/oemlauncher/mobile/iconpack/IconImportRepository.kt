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
import java.util.UUID

data class IconMatch(val app: CatalogEntry, val iconId: String)

/** [session] names the staging directory holding the icons until the user confirms. */
data class ImportPreview(val session: String, val matches: List<IconMatch>, val unmatched: List<String>)
class IconOverrideLimitException : Exception()

class IconImportRepository(
    private val resolver: ContentResolver,
    private val repository: CompanionRepository,
    private val processor: IconProcessor,
) {
    /** Session of the preview the user is still reviewing; GC must not delete it. */
    private var activeSession: String? = null

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
            collectGarbage()
        }

    suspend fun import(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val session = UUID.randomUUID().toString()
        try {
            val pack = requireNotNull(resolver.openInputStream(uri)).use { ZipArchiveReader().read(it) }
            val entries = repository.state.value.catalog?.entries.orEmpty()
            val normalized = LinkedHashMap<String, String>()
            for ((name, raw) in pack.images) {
                normalized[name] = repository.icons.putStaged(session, processor.normalize(raw))
            }
            val used = HashSet<String>()
            val matches = entries.mapNotNull { app ->
                val componentImage = pack.filters[app.key]?.takeIf { it in normalized }
                val packageImage = AppKey.parse(app.key)?.packageName?.takeIf { it in normalized }
                val name = componentImage ?: packageImage ?: return@mapNotNull null
                used += name
                IconMatch(app, normalized.getValue(name))
            }
            ImportPreview(session, matches, normalized.keys.filterNot { it in used })
                .also { activeSession = session }
        } catch (e: Exception) {
            repository.icons.discard(session)
            throw e
        }
    }

    suspend fun apply(preview: ImportPreview) = withContext(Dispatchers.IO) {
        try {
            repository.edit { config ->
                checkLimit(config.iconOverrides.keys + preview.matches.map { it.app.key })
                preview.matches.fold(config) { current, match ->
                    ConfigEditor.setIconOverride(current, match.app.key, match.iconId)
                }
            }
            repository.icons.promote(preview.session, preview.matches.map { it.iconId })
            repository.iconsChanged()
        } finally {
            if (activeSession == preview.session) activeSession = null
            repository.icons.discard(preview.session)
            collectGarbage()
        }
    }

    /** User cancelled the review: drop every staged icon of this session. */
    suspend fun discard(preview: ImportPreview) = withContext(Dispatchers.IO) {
        if (activeSession == preview.session) activeSession = null
        repository.icons.discard(preview.session)
        collectGarbage()
    }

    suspend fun reset(key: String) = repository.edit { ConfigEditor.setIconOverride(it, key, null) }
        .also { collectGarbage() }

    suspend fun resetAll() = repository.edit { config ->
        config.iconOverrides.keys.fold(config) { current, key ->
            ConfigEditor.setIconOverride(current, key, null)
        }
    }.also { collectGarbage() }

    /** Referenced icons include the catalog, every draft history entry and the outbox. */
    private suspend fun collectGarbage() {
        val snapshot = repository.state.value.snapshot
        val keep = HashSet<String>()
        snapshot.catalog?.entries?.forEach { it.iconId?.let(keep::add) }
        snapshot.draft?.let { draft ->
            (listOf(draft.current, draft.baseline) + draft.undo + draft.redo).forEach {
                keep.addAll(it.iconOverrides.values)
            }
        }
        snapshot.sent?.let { keep.addAll(it.iconOverrides.values) }
        snapshot.watch?.current?.let { keep.addAll(it.iconOverrides.values) }
        repository.icons.collectGarbage(keep, keepSessions = setOfNotNull(activeSession))
    }

    private fun checkLimit(keys: Set<String>) {
        if (keys.size > LauncherDefaults.MAX_ICON_OVERRIDES) throw IconOverrideLimitException()
    }
}
