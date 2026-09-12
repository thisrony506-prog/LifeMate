package com.lifemate.updates

import android.content.Context
import com.lifemate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** No record/profile access, auth token, identifier or analytics. HTTPS metadata only. */
class UpdateRepository(context: Context) {
    private val cache = context.getSharedPreferences("release_updates", Context.MODE_PRIVATE)
    fun cached(): ReleaseInfo? = try { parse(cache.getString("metadata", null) ?: "") } catch (_: Exception) { null }
    fun due(now: Long = System.currentTimeMillis()): Boolean {
        val last = cache.getLong("attempt", 0)
        return now < last || now - last >= 6 * 60 * 60 * 1000L
    }
    suspend fun fetch(): ReleaseInfo? = withContext(Dispatchers.IO) {
        cache.edit().putLong("attempt", System.currentTimeMillis()).apply()
        val connection = URL(ReleaseInfo.API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "LifeMate/${BuildConfig.VERSION_NAME}")
            when (connection.responseCode) {
                404 -> { cache.edit().remove("metadata").apply(); return@withContext null }
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
            cache.edit().putString("metadata", text).apply()
            info
        } finally { connection.disconnect() }
    }
    private fun parse(text: String): ReleaseInfo? {
        val release = JSONObject(text)
        if (release.optBoolean("draft", true) || release.optBoolean("prerelease", true)) return null
        val tag = release.optString("tag_name")
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optLong("size") !in 1..209_715_200L) continue
            ReleaseInfo.validated(tag, asset.optString("name"), asset.optString("browser_download_url"))?.let { return it }
        }
        return null
    }
}

data class UpdateState(val release: ReleaseInfo? = null, val checking: Boolean = false, val message: String? = null) {
    val available get() = release?.newerThan(BuildConfig.VERSION_CODE) == true
}
