package com.n2.oemlauncher.mobile.search

import com.n2.oemlauncher.mobile.config.SearchConstants
import com.n2.oemlauncher.shared.model.CatalogEntry

/**
 * Search keys per app, precomputed so typing never romanizes labels. Besides its label an app
 * matches its full pinyin ("weixin") and its initials ("wx"); a polyphonic character contributes
 * every reading because the dictionary cannot tell which one a label uses.
 */
class AppSearchIndex private constructor(private val keys: Map<String, List<String>>) {

    /** Case and whitespace are ignored; apps missing from the index still match by label. */
    fun matches(entry: CatalogEntry, query: String): Boolean {
        val needle = normalize(query)
        if (needle.isEmpty() || normalize(entry.label).contains(needle)) return true
        return keys[entry.key]?.any { it.contains(needle) } == true
    }

    companion object {
        val EMPTY = AppSearchIndex(emptyMap())

        fun build(entries: List<CatalogEntry>, romanizer: Romanizer): AppSearchIndex =
            AppSearchIndex(entries.associate { it.key to keysFor(it.label, romanizer) })

        internal fun keysFor(label: String, romanizer: Romanizer): List<String> {
            val tokens = tokenize(label, romanizer)
            if (tokens.isEmpty()) return emptyList()
            val initials = tokens.map { spellings -> spellings.map { it.take(1) }.distinct() }
            return (combine(tokens) + combine(initials)).distinct()
        }

        /** Each token lists its alternative spellings: a Han character's readings or one Latin word. */
        private fun tokenize(label: String, romanizer: Romanizer): List<List<String>> {
            val tokens = ArrayList<List<String>>()
            val word = StringBuilder()
            fun flushWord() {
                if (word.isNotEmpty()) tokens += listOf(word.toString().lowercase())
                word.clear()
            }
            for (char in label) {
                val readings = romanizer.readings(char)
                when {
                    readings != null -> {
                        flushWord()
                        tokens += readings
                    }
                    char.isLetterOrDigit() -> word.append(char)
                    else -> flushWord()
                }
            }
            flushWord()
            return tokens
        }

        private fun combine(tokens: List<List<String>>): List<String> =
            tokens.fold(listOf("")) { prefixes, spellings ->
                prefixes.flatMap { prefix -> spellings.map { prefix + it } }
                    .distinct()
                    .take(SearchConstants.MAX_SPELLINGS)
            }

        private fun normalize(text: String): String = text.filterNot(Char::isWhitespace).lowercase()
    }
}
