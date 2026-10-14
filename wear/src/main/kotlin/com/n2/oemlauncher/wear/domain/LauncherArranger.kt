package com.n2.oemlauncher.wear.domain

import com.n2.oemlauncher.shared.model.CatalogEntry
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.ordering.AppOrdering

object LauncherArranger {
    fun arrange(entries: List<CatalogEntry>, config: LauncherConfig): List<CatalogEntry> =
        AppOrdering.arrange(entries, config).all
}
