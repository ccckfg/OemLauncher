package com.n2.oemlauncher.wear.catalog

/** One delayed retry covers LauncherApps' first-refresh race after process or permission changes. */
fun shouldRetryEmptyCatalog(entryCount: Int): Boolean = entryCount == 0
