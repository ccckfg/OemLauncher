package com.n2.oemlauncher.wear.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemHomeSelectorTest {
    private fun candidate(packageName: String, system: Boolean = true, exported: Boolean = true, enabled: Boolean = true) =
        HomeCandidate(packageName, "$packageName.Home", system, exported, enabled)

    @Test fun filtersOwnFallbackAndInvalidCandidates() {
        val selected = selectSystemHome(
            listOf(candidate("com.n2.oemlauncher"), HomeCandidate("com.android.settings", "com.android.settings.FallbackHome", true, true, true),
                candidate("com.third.party", system = false), candidate("com.disabled", enabled = false), candidate("com.watch.face")),
            ownPackage = "com.n2.oemlauncher",
        )
        assertEquals("com.watch.face", selected?.packageName)
    }

    @Test fun returnsNullWhenNoSystemHomeExists() {
        assertNull(selectSystemHome(listOf(candidate("com.example", system = false)), "com.n2.oemlauncher"))
    }

    @Test fun filtersFallbackHomeByClassName() {
        val selected = selectSystemHome(
            listOf(
                HomeCandidate("com.android.settings", "com.android.settings.FallbackHome", true, true, true),
                HomeCandidate("com.watch.face", "com.watch.FaceHome", true, true, true),
            ),
            ownPackage = "com.n2.oemlauncher",
        )
        assertEquals("com.watch.face", selected?.packageName)
    }
}
