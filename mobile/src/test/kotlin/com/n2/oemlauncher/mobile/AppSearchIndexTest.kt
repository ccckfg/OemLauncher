package com.n2.oemlauncher.mobile

import com.n2.oemlauncher.mobile.config.SearchConstants
import com.n2.oemlauncher.mobile.search.AppSearchIndex
import com.n2.oemlauncher.mobile.search.PinyinRomanizer
import com.n2.oemlauncher.mobile.search.Romanizer
import com.n2.oemlauncher.shared.model.CatalogEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchIndexTest {
    private val pinyin = PinyinRomanizer()
    private fun entry(label: String) = CatalogEntry("app/$label", label)
    private fun index(vararg labels: String) = AppSearchIndex.build(labels.map(::entry), pinyin)

    private fun assertMatches(label: String, vararg queries: String) {
        val index = index(label)
        queries.forEach { assertTrue("$label should match $it", index.matches(entry(label), it)) }
    }

    @Test fun chineseLabelMatchesInitialsFullPinyinAndText() {
        assertMatches("微信", "wx", "WX", "weixin", "wei xin", "weix", "微信", "信")
        assertMatches("联系人", "lxr", "lianxiren", "lian")
        assertMatches("电话", "dh", "dianhua")
        assertFalse(index("微信").matches(entry("微信"), "zfb"))
    }

    @Test fun polyphonicCharactersMatchEveryReading() {
        assertMatches("银行", "yh", "yinhang", "yx", "yinxing")
    }

    @Test fun umlautIsTypedAsV() {
        assertMatches("绿洲", "lvzhou", "lz")
    }

    @Test fun latinWordsContributeInitials() {
        assertMatches("Google Maps", "gm", "maps", "googlem")
        assertMatches("QQ音乐", "qqyinyue", "qyy")
        assertFalse(index("Google Maps").matches(entry("Google Maps"), "gx"))
    }

    @Test fun blankQueryMatchesEverything() {
        assertMatches("微信", "", "  ")
    }

    @Test fun appsMissingFromTheIndexStillMatchTheirLabel() {
        assertTrue(AppSearchIndex.EMPTY.matches(entry("微信"), "微"))
        assertFalse(AppSearchIndex.EMPTY.matches(entry("微信"), "wx"))
    }

    @Test fun spellingsPerLabelAreCapped() {
        val threeReadings = Romanizer { char -> listOf("a$char", "b$char", "c$char") }
        val keys = AppSearchIndex.keysFor("一二三四五六七八九十", threeReadings)
        assertTrue(keys.size <= 2 * SearchConstants.MAX_SPELLINGS)
    }
}
