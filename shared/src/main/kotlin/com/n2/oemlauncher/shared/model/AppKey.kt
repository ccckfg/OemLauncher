package com.n2.oemlauncher.shared.model

/**
 * Identity of one launchable activity.
 *
 * The flattened form `package/fully.qualified.Class` (what `ComponentName.flattenToString()`
 * returns) is the only key used in configs and sync payloads. Keys that come from outside must
 * go through [canonical] so that short forms such as `pkg/.Main` never create duplicates.
 */
data class AppKey(val packageName: String, val className: String) {

    fun flatten(): String = packageName + SEPARATOR + className

    companion object {
        private const val SEPARATOR = '/'
        private const val MAX_LENGTH = 512
        private val PACKAGE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*$")
        private val CLASS_PATTERN =
            Regex("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$")

        fun parse(flat: String): AppKey? {
            if (flat.isEmpty() || flat.length > MAX_LENGTH) return null
            val split = flat.indexOf(SEPARATOR)
            if (split <= 0 || split == flat.lastIndex) return null
            val packageName = flat.substring(0, split)
            val rawClass = flat.substring(split + 1)
            val className = if (rawClass.startsWith('.')) packageName + rawClass else rawClass
            if (!PACKAGE_PATTERN.matches(packageName) || !CLASS_PATTERN.matches(className)) {
                return null
            }
            return AppKey(packageName, className)
        }

        /** Canonical flattened key, or null when [flat] is not a valid component name. */
        fun canonical(flat: String): String? = parse(flat)?.flatten()
    }
}
