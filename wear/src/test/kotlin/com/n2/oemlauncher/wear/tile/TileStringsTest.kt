package com.n2.oemlauncher.wear.tile

import android.app.Application
import android.app.LocaleManager
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.n2.oemlauncher.wear.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en")
class TileStringsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun explicitAppLanguageOverridesAnEnglishTileHost() {
        val strings = tileStringContext(context, LocaleList.forLanguageTags("zh-CN"))
        assertEquals("常用应用", strings.getString(R.string.tile_header))
        assertEquals("还没有常用应用", strings.getString(R.string.tile_empty))
        assertEquals("打开启动器", strings.getString(R.string.tile_open_launcher))
    }

    @Test fun defaultLanguageKeepsTheHostContext() {
        assertSame(context, tileStringContext(context, LocaleList.getEmptyLocaleList()))
    }

    @Test fun readsTheApplicationLanguageFromLocaleManager() {
        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("zh-CN")
        assertEquals("常用应用", tileStringContext(context).getString(R.string.tile_header))
    }
}
