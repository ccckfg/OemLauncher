package com.n2.oemlauncher.wear.catalog

import android.graphics.Bitmap
import android.os.UserHandle
import com.n2.oemlauncher.shared.model.CatalogEntry

data class LaunchableApp(
    val entry: CatalogEntry,
    val user: UserHandle,
    val bitmap: Bitmap?,
)
