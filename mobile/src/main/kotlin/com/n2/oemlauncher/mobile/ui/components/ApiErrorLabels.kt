package com.n2.oemlauncher.mobile.ui.components

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wearable.WearableStatusCodes
import com.n2.oemlauncher.mobile.R

/**
 * Maps an ApiException status code to a localized phrase. Only the code is kept: exception
 * messages and node identifiers never reach the UI. Null stands for a non-API failure.
 */
object ApiErrorLabels {
    fun labelRes(statusCode: Int?): Int = when (statusCode) {
        null -> R.string.diag_error_local
        ConnectionResult.API_UNAVAILABLE, ConnectionResult.SERVICE_MISSING,
        ConnectionResult.SERVICE_DISABLED, ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
        ConnectionResult.SERVICE_INVALID,
        -> R.string.diag_error_unavailable
        WearableStatusCodes.TARGET_NODE_NOT_CONNECTED, WearableStatusCodes.INVALID_TARGET_NODE,
        CommonStatusCodes.API_NOT_CONNECTED,
        -> R.string.diag_error_not_connected
        ConnectionResult.NETWORK_ERROR, ConnectionResult.TIMEOUT,
        CommonStatusCodes.NETWORK_ERROR, CommonStatusCodes.TIMEOUT,
        -> R.string.diag_error_timeout
        WearableStatusCodes.DATA_ITEM_TOO_LARGE -> R.string.diag_error_too_large
        else -> R.string.diag_error_unknown_code
    }

    /** True when the phrase needs the numeric code appended ("Communication error (code N)"). */
    fun needsCode(statusCode: Int?): Boolean = statusCode != null &&
        labelRes(statusCode) == R.string.diag_error_unknown_code
}
