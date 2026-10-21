package com.n2.oemlauncher.wear.tile

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList

/** The Tile host supplies its own configuration, which can differ from the app's language. */
internal fun tileStringContext(context: Context, locales: LocaleList = appLocales(context)): Context {
    if (locales.isEmpty) return context
    val configuration = Configuration(context.resources.configuration).apply { setLocales(locales) }
    return context.createConfigurationContext(configuration)
}

private fun appLocales(context: Context): LocaleList =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales
    } else LocaleList.getEmptyLocaleList()
