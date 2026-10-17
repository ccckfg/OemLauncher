package com.n2.oemlauncher.wear.home

data class HomeCandidate(
    val packageName: String,
    val className: String,
    val isSystemApp: Boolean,
    val exported: Boolean,
    val enabled: Boolean,
)

/** Pure filtering used to keep PackageManager details out of the Home policy tests. */
fun selectSystemHome(
    candidates: List<HomeCandidate>,
    ownPackage: String,
    fallbackClassName: String = "com.android.settings.FallbackHome",
): HomeCandidate? = candidates.firstOrNull { candidate ->
    candidate.packageName != ownPackage &&
        candidate.className != fallbackClassName &&
        candidate.isSystemApp && candidate.exported && candidate.enabled
}
