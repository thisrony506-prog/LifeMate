package com.lifemate.updates

/** Only immutable, versioned APK assets in the official public repository are accepted. */
data class ReleaseInfo(val code: Int, val version: String, val downloadUrl: String) {
    fun newerThan(installed: Int) = code > installed
    companion object {
        const val REPOSITORY = "thisrony506-prog/LifeMate"
        const val API = "https://api.github.com/repos/$REPOSITORY/releases/latest"
        fun validated(tag: String, assetName: String, url: String): ReleaseInfo? {
            val build = Regex("v1\\.2\\.([1-9][0-9]{0,6})").matchEntire(tag)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            if (build < 1) return null
            val code = 100_000 + build
            if (assetName != "LifeMate-$code.apk") return null
            if (url != "https://github.com/$REPOSITORY/releases/download/$tag/$assetName") return null
            return ReleaseInfo(code, tag.removePrefix("v"), url)
        }
    }
}

/** Both inputs must already have passed installed-certificate verification. Never forget a known required update on a stale response. */
fun retainNewestRelease(known: ReleaseInfo?, incoming: ReleaseInfo?): ReleaseInfo? = when {
    incoming == null -> known
    known != null && known.code > incoming.code -> known
    else -> incoming
}
