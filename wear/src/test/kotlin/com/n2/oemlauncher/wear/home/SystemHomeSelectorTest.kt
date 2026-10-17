package com.n2.oemlauncher.wear.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemHomeSelectorTest {
    private fun candidate(packageName: String, system: Boolean = true, exported: Boolean = true, enabled: Boolean = true) =
        HomeCandidate(packageName, "$packageName.Home", system, exported, enabled)

    @Test fun filtersOwnFallbackAndInvalidCandidates() {
        val selected = selectSystemHome(
            listOf(candidate("com.n2.oemlauncher"), candidate("com.android.settings.FallbackHome"),
                candidate("com.third.party", system = false), candidate("com.disabled", enabled = false), candidate("com.watch.face")),
            ownPackage = "com.n2.oemlauncher",
        )
        assertEquals("com.watch.face", selected?.packageName)
    }

    @Test fun returnsNullWhenNoSystemHomeExists() {
        assertNull(selectSystemHome(listOf(candidate("com.example", system = false)), "com.n2.oemlauncher"))
    }
}
