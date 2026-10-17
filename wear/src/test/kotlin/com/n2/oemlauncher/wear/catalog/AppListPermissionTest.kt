package com.n2.oemlauncher.wear.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class AppListPermissionTest {
    private fun status(available: Boolean, granted: Boolean, rationale: Boolean, attempted: Boolean) =
        AppListPermissionStatus(available, granted, rationale, attempted)

    @Test fun unavailableUsesGenericEmptyState() = assertEquals(
        EmptyCatalogState.GENERIC, emptyCatalogState(status(false, false, false, false)),
    )

    @Test fun deniedPermissionNeedsRequest() = assertEquals(
        EmptyCatalogState.NEEDS_PERMISSION, emptyCatalogState(status(true, false, true, false)),
    )

    @Test fun permanentlyDeniedPermissionUsesSettings() = assertEquals(
        EmptyCatalogState.PERMISSION_BLOCKED, emptyCatalogState(status(true, false, false, true)),
    )

    @Test fun grantedPermissionUsesGenericEmptyState() = assertEquals(
        EmptyCatalogState.GENERIC, emptyCatalogState(status(true, true, false, true)),
    )
}
