package com.n2.oemlauncher.mobile.search

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

/** Romanized readings of one character, or null when it is not a Han character. */
fun interface Romanizer {
    fun readings(char: Char): List<String>?
}

/**
 * Toneless lowercase pinyin with "ü" written as "v", which is how Chinese keyboards type it.
 * pinyin4j loads its whole dictionary on first use, so callers must stay off the main thread.
 */
class PinyinRomanizer : Romanizer {
    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    override fun readings(char: Char): List<String>? = try {
        PinyinHelper.toHanyuPinyinStringArray(char, format)?.distinct()?.takeIf { it.isNotEmpty() }
    } catch (_: BadHanyuPinyinOutputFormatCombination) {
        null
    }
}
