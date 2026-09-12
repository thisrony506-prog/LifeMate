package com.lifemate

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.updates.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ApkDownloadTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory get() = File(context.cacheDir,"updates")
    private val bytes = "controlled APK transfer fixture".toByteArray()
    private fun release(data: ByteArray = bytes): ReleaseInfo {
        val next = BuildConfig.VERSION_CODE-100000+1
        return ReleaseInfo(100000+next,"1.2.$next","https://github.com/thisrony506-prog/LifeMate/releases/download/v1.2.$next/LifeMate-${100000+next}.apk",data.size.toLong(),MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) },"cd".repeat(32))
    }
    private class Response(private val status: Int = 200, private val data: ByteArray = byteArrayOf(), private val location: String? = null, private val length: Long = data.size.toLong()): HttpURLConnection(URL("https://github.com/fixture")) {
        var closed = false
        override fun connect() {}
        override fun disconnect() { closed=true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getHeaderField(name: String?) = if(name=="Location") location else null
        override fun getContentLengthLong() = length
        override fun getInputStream() = ByteArrayInputStream(data)
    }
    @Before fun reset() { directory.deleteRecursively() }
    @After fun clean() { directory.deleteRecursively() }
    @Test fun verifiedTransferReportsProgressAndReusesRecheckedCache() = runBlocking {
        var requests=0; var checked=0
        val updates=mutableListOf<ApkDownloadState>()
        val downloader=ApkUpdateDownloader(context,{ requests++; Response(data=bytes) },{ _,_ -> checked++ })
        val file=downloader.download(release()) { updates.add(it) }
        assertArrayEquals(bytes,file.readBytes())
        assertTrue(updates.any { it.phase==DownloadPhase.DOWNLOADING && it.received==bytes.size.toLong() })
        assertTrue(updates.any { it.phase==DownloadPhase.VERIFYING })
        downloader.download(release()) {}
        assertEquals(1,requests); assertEquals(2,checked)
        assertFalse(directory.listFiles().orEmpty().any { ".part." in it.name })
        Unit
    }
    @Test fun officialRedirectWorksButForeignRedirectNeverConnects() = runBlocking {
        var connections=0
        val downloader=ApkUpdateDownloader(context,{ url -> connections++; if(url.host=="github.com") Response(302,location="https://release-assets.githubusercontent.com/fixture") else Response(data=bytes) },{ _,_ -> })
        downloader.download(release()) {}; assertEquals(2,connections)
        directory.deleteRecursively(); connections=0
        try {
            ApkUpdateDownloader(context,{ connections++; Response(302,location="https://evil.example/payload") },{ _,_ -> }).download(release()) {}
            fail("Foreign redirect must fail")
        } catch (_: IllegalStateException) { }
        assertEquals(1,connections)
    }
    @Test fun tamperedTruncatedAndOversizeFilesNeverBecomeReady() = runBlocking {
        for (body in listOf(bytes.copyOf().apply { this[0]=0 },bytes.copyOf(2),bytes+byteArrayOf(1))) {
            try { ApkUpdateDownloader(context,{ Response(data=body,length=-1) },{ _,_ -> fail("Must not inspect unverified bytes") }).download(release()) {}; fail("Must reject size/hash mismatch") }
            catch (_: IllegalStateException) { }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        }
    }
    @Test fun correctHashDoesNotMakeNonApkInstallable() = runBlocking {
        try { ApkUpdateDownloader(context,{ Response(data=bytes) }).download(release()) {}; fail("Android must reject non-APK bytes") }
        catch (_: IllegalStateException) { }
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }
    @Test fun cancellationRemovesPartialFileAndDisconnects() = runBlocking {
        val response=Response(data=bytes)
        val child=launch {
            ApkUpdateDownloader(context,{ response },{ _,_ -> }).download(release()) { if(it.phase==DownloadPhase.DOWNLOADING) cancel() }
        }
        child.join()
        assertTrue(child.isCancelled); assertTrue(response.closed)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }
    @Test fun cachedTamperingFailsBeforeInstallerHandoff() = runBlocking {
        val downloader=ApkUpdateDownloader(context,{ Response(data=bytes) },{ _,_ -> })
        val file=downloader.download(release()) {}
        file.writeBytes(bytes.copyOf().apply { this[0]=0 })
        try { downloader.verifyReady(release()); fail("Must reverify before install") }
        catch (_: IllegalStateException) { }
        assertFalse(file.exists())
    }
    @Test fun installerUsesScopedContentUriNotBrowserOrFileUri() {
        directory.mkdirs()
        val file=File(directory,"LifeMate-100001.apk").apply { writeBytes(bytes) }
        val intent=nativeUpdateIntent(context,file)
        @Suppress("DEPRECATION") assertEquals(Intent.ACTION_INSTALL_PACKAGE,intent.action)
        assertEquals("content",intent.data!!.scheme)
        assertEquals("application/vnd.android.package-archive",intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertArrayEquals(bytes,context.contentResolver.openInputStream(intent.data!!)!!.use { it.readBytes() })
        try { nativeUpdateIntent(context,File(directory,"LifeMate-100001.part.apk").apply { writeBytes(bytes) }); fail("Partial file must not be shared for installation") }
        catch (_: IllegalArgumentException) { }
    }
}
