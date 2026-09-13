package com.lifemate.updates

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import com.lifemate.BuildConfig
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Foreground, bounded, app-private download. Nothing is executable until signed bytes are verified. */
class ApkUpdateDownloader(
    private val context: Context,
    private val connect: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val archiveCheck: (File, ReleaseInfo) -> Unit = { file, release -> verifyApkArchive(context,file,release) }
) {
    private val directory get() = File(context.cacheDir,"updates").apply { check(mkdirs() || isDirectory) { "Update storage is unavailable." } }
    private fun target(info: ReleaseInfo) = File(directory,"LifeMate-${info.code}.apk")

    suspend fun verifyReady(info: ReleaseInfo): File = withContext(Dispatchers.IO) {
        check(ApkDownloadPolicy.validProof(info,BuildConfig.VERSION_CODE)) { "No verified newer update is available." }
        val file = target(info)
        try { verifyBytes(file,info); archiveCheck(file,info); file }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { file.delete(); throw e }
    }

    suspend fun download(info: ReleaseInfo, progress: (ApkDownloadState) -> Unit): File = withContext(Dispatchers.IO) {
        check(ApkDownloadPolicy.validProof(info,BuildConfig.VERSION_CODE)) { "The update proof is invalid. Check for updates again." }
        val output = target(info)
        // Completed files survive activity/process recreation, but are rechecked, never blindly trusted.
        if (output.exists()) {
            progress(ApkDownloadState(info.code,DownloadPhase.VERIFYING,info.bytes,info.bytes))
            try { return@withContext verifyReady(info) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Discard corrupted cache and start a fresh verified download. */ }
        }
        directory.listFiles()?.filter { it.name != output.name }?.forEach { it.delete() }
        check(StatFs(directory.path).availableBytes >= info.bytes + 2*1024*1024) { "Not enough free storage. Free space and retry." }
        val partial = File(directory,"LifeMate-${info.code}.part.apk")
        try {
            withTimeout(10*60*1000L) {
                var url = URL(info.downloadUrl)
                var redirects = 0
                while (true) {
                    ensureActive()
                    check(ApkDownloadPolicy.allowed(url.toString())) { "An unsafe download redirect was blocked." }
                    val connection = connect(url)
                    try {
                        connection.instanceFollowRedirects = false
                        connection.connectTimeout = 15_000; connection.readTimeout = 15_000
                        connection.useCaches = false
                        connection.setRequestProperty("Accept-Encoding","identity")
                        connection.setRequestProperty("User-Agent","LifeMate/${BuildConfig.VERSION_NAME}")
                        val status = connection.responseCode
                        if (status in setOf(301,302,303,307,308)) {
                            check(++redirects <= 5) { "Too many download redirects. Retry later." }
                            val location = connection.getHeaderField("Location") ?: error("Download redirect is missing.")
                            url = URL(url,location)
                            continue
                        }
                        check(status == 200) { "The download service is unavailable. Retry when connected." }
                        val length = connection.contentLengthLong
                        check(length == -1L || length == info.bytes) { "The APK size does not match its signed proof." }
                        val digest = MessageDigest.getInstance("SHA-256")
                        var received = 0L; var reported = 0L
                        progress(ApkDownloadState(info.code,DownloadPhase.DOWNLOADING,0,info.bytes))
                        connection.inputStream.use { input -> partial.outputStream().use { stream ->
                            val buffer = ByteArray(65536)
                            while (true) {
                                ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                received += n
                                check(received <= info.bytes) { "The APK exceeds its signed size." }
                                stream.write(buffer,0,n); digest.update(buffer,0,n)
                                if (received-reported >= 131072 || received == info.bytes) {
                                    reported = received
                                    progress(ApkDownloadState(info.code,DownloadPhase.DOWNLOADING,received,info.bytes))
                                }
                            }
                        } }
                        check(received == info.bytes && digest.digest().hex() == info.sha256) { "APK verification failed. Retry the official download." }
                        progress(ApkDownloadState(info.code,DownloadPhase.VERIFYING,received,info.bytes))
                        archiveCheck(partial,info)
                        ensureActive()
                        check(partial.renameTo(output)) { "Could not save the verified update." }
                        return@withTimeout output
                    } finally { connection.disconnect() }
                }
                @Suppress("UNREACHABLE_CODE") error("Download did not complete.")
            }
        } finally { partial.delete() }
    }

    private suspend fun verifyBytes(file: File, info: ReleaseInfo) {
        check(file.isFile && file.length() == info.bytes) { "Download the update again; its saved file is missing or incomplete." }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { currentCoroutineContext().ensureActive(); val n=input.read(buffer); if(n<0) break; digest.update(buffer,0,n) }
        }
        check(digest.digest().hex() == info.sha256) { "The saved APK failed verification. Download it again." }
    }
}

internal fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

/** Hash against signed metadata proves the exact trusted bytes; Android performs final APK signature validation. */
@Suppress("DEPRECATION")
fun verifyApkArchive(context: Context, file: File, release: ReleaseInfo) {
    val manager = context.packageManager
    val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val installed = manager.getPackageInfo(context.packageName,flags)
    val apk = manager.getPackageArchiveInfo(file.path,flags) ?: error("Android cannot read this APK.")
    val code = if (Build.VERSION.SDK_INT >= 28) apk.longVersionCode else apk.versionCode.toLong()
    val installedCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
    check(apk.packageName == context.packageName && code == release.code.toLong() && code > installedCode && apk.versionName == release.version) { "This APK is not the required LifeMate update." }
    check((apk.applicationInfo?.flags ?: error("APK information is missing.")) and ApplicationInfo.FLAG_DEBUGGABLE == 0) { "Debug APKs cannot be installed as official updates." }
    val currentSigners = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
    val newSigners = if (Build.VERSION.SDK_INT >= 28) apk.signingInfo?.apkContentsSigners else apk.signatures
    val current = currentSigners?.singleOrNull()?.toByteArray() ?: error("Installed signer is unavailable.")
    val incoming = newSigners?.singleOrNull()?.toByteArray() ?: error("Update signer is unavailable.")
    check(current.contentEquals(incoming) && MessageDigest.getInstance("SHA-256").digest(incoming).hex() == release.signerSha256) { "The update signing key does not match LifeMate." }
}
