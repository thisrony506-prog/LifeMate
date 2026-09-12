package com.lifemate.updates

import java.net.URI

/** Public GitHub release redirects only. No cleartext, arbitrary hosts, credentials or custom ports. */
object ApkDownloadPolicy {
    private val hosts = setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
    fun allowed(url: String): Boolean = try {
        val uri = URI(url)
        uri.scheme == "https" && uri.host in hosts && uri.rawUserInfo == null && uri.port in setOf(-1,443) && uri.rawFragment == null
    } catch (_: Exception) { false }
    fun validProof(info: ReleaseInfo, installedCode: Int): Boolean =
        info.newerThan(installedCode) && info.bytes in 1..209_715_200L && Regex("[a-f0-9]{64}").matches(info.sha256) &&
            Regex("[a-f0-9]{64}").matches(info.signerSha256) &&
            ReleaseInfo.validated("v${info.version}","LifeMate-${info.code}.apk",info.downloadUrl)?.code == info.code
}

enum class DownloadPhase { IDLE, DOWNLOADING, VERIFYING, READY, ERROR }
data class ApkDownloadState(val code: Int = 0, val phase: DownloadPhase = DownloadPhase.IDLE, val received: Long = 0, val total: Long = 0, val message: String? = null) {
    val busy get() = phase == DownloadPhase.DOWNLOADING || phase == DownloadPhase.VERIFYING
    val progress get() = if (total <= 0) 0f else (received.toFloat()/total).coerceIn(0f,1f)
}
