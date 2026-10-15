package com.n2.oemlauncher.mobile

import com.n2.oemlauncher.mobile.ui.components.ApiErrorLabels
import com.n2.oemlauncher.mobile.ui.components.StatusTone
import com.n2.oemlauncher.mobile.ui.components.tone
import com.n2.oemlauncher.shared.protocol.SyncPhase
import org.junit.Assert.*
import org.junit.Test

class ApiErrorLabelsTest {
    @Test fun knownCodesMapToFixedPhrases() {
        assertEquals(R.string.diag_error_unavailable, ApiErrorLabels.labelRes(16)) // API_UNAVAILABLE
        assertEquals(R.string.diag_error_unavailable, ApiErrorLabels.labelRes(1)) // SERVICE_MISSING
        assertEquals(R.string.diag_error_not_connected, ApiErrorLabels.labelRes(4000))
        assertEquals(R.string.diag_error_timeout, ApiErrorLabels.labelRes(7)) // NETWORK_ERROR
        assertEquals(R.string.diag_error_timeout, ApiErrorLabels.labelRes(14)) // TIMEOUT
        assertEquals(R.string.diag_error_too_large, ApiErrorLabels.labelRes(4003))
        assertEquals(R.string.diag_error_local, ApiErrorLabels.labelRes(null))
    }

    @Test fun unknownCodesFallBackToANumericPhrase() {
        val code = 987654
        assertEquals(R.string.diag_error_unknown_code, ApiErrorLabels.labelRes(code))
        assertTrue(ApiErrorLabels.needsCode(code))
        assertFalse(ApiErrorLabels.needsCode(16))
        assertFalse(ApiErrorLabels.needsCode(null))
    }

    /** Every phase a user can see has a tone, so the card never renders a blank state. */
    @Test fun everySyncPhaseHasATone() {
        SyncPhase.entries.forEach { phase ->
            assertTrue(phase.name, phase.tone() in StatusTone.entries)
        }
    }
}
