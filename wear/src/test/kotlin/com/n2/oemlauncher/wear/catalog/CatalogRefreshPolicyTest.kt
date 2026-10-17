package com.n2.oemlauncher.wear.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRefreshPolicyTest {
    @Test fun retriesOnlyAnEmptySuccessfulQuery() {
        assertTrue(shouldRetryEmptyCatalog(0))
        assertFalse(shouldRetryEmptyCatalog(1))
    }
}
