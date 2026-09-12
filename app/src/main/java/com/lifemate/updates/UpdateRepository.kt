package com.lifemate.updates

import android.content.Context
import com.lifemate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.pm.PackageManager
import android.os.Build
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.net.HttpURLConnection
import java.net.URL

/** No record/profile access, auth token, identifier or analytics. HTTPS metadata only. */
class UpdateRepository(
    private val context: Context,
    private val connect: () -> HttpURLConnection = { URL(ReleaseInfo.API).openConnection() as HttpURLConnection }
) {
    private val cache = context.getSharedPreferences("release_updates", Context.MODE_PRIVATE)
    fun cached(): ReleaseInfo? = try { parse(cache.getString("metadata", null) ?: "") } catch (_: Exception) { null }
    fun due(now: Long = System.currentTimeMillis()): Boolean {
        val last = cache.getLong("attempt", 0)
        return UpdateCadence.due(now, last)
    }
    suspend fun fetch(): ReleaseInfo? = withContext(Dispatchers.IO) {
        cache.edit().putLong("attempt", System.currentTimeMillis()).apply()
        val connection = connect()
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "LifeMate/${BuildConfig.VERSION_NAME}")
            when (connection.responseCode) {
                404 -> return@withContext cached()
                403, 429 -> error("Update service is busy. Try again later.")
                200 -> Unit
                else -> error("The update service is unavailable. Try again later.")
            }
            val bytes = connection.inputStream.use { stream ->
                val result = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    check(result.size() + count <= 262_144) { "Unexpected update response." }
                    result.write(buffer, 0, count)
                }
                result.toByteArray()
            }
            val text = bytes.toString(Charsets.UTF_8)
            val info = parse(text) ?: error("No compatible signed release is published yet.")
            val known = cached()
            if (retainNewestRelease(known, info) != info) return@withContext known
            cache.edit().putString("metadata", text).apply()
            info
        } finally { connection.disconnect() }
    }
    @Suppress("DEPRECATION")
    private fun parse(text: String): ReleaseInfo? {
        val manager = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        } else manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        val bytes = signatures?.singleOrNull()?.toByteArray() ?: return null
        val certificate = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(bytes))
        val signer = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return ReleaseMetadata.parse(text, signer, certificate.publicKey)
    }
}

data class UpdateState(val release: ReleaseInfo? = null, val checking: Boolean = false, val message: String? = null) {
    val available get() = release?.newerThan(BuildConfig.VERSION_CODE) == true
}
