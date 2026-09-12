package com.lifemate

import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.updates.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class UpdateMetadataTest {
    companion object {
        private val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        private val signer = "ab".repeat(32)
        private val sha = "cd".repeat(32)
        private val info = ReleaseInfo.validated("v1.2.7", "LifeMate-100007.apk", "https://github.com/thisrony506-prog/LifeMate/releases/download/v1.2.7/LifeMate-100007.apk")!!
    }
    private fun metadata(): JSONObject {
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(pair.private); update(ReleaseProof.canonical(info,1234,sha,signer)); sign()
        }
        val proof = JSONObject().put("code",info.code).put("version",info.version).put("url",info.downloadUrl)
            .put("size",1234).put("sha256",sha).put("signer",signer).put("signature",Base64.getEncoder().encodeToString(signature))
        val asset = JSONObject().put("name","LifeMate-100007.apk").put("browser_download_url",info.downloadUrl)
            .put("size",1234).put("state","uploaded").put("content_type","application/vnd.android.package-archive").put("digest","sha256:$sha")
        return JSONObject().put("draft",false).put("prerelease",false).put("tag_name","v1.2.7")
            .put("body","<!-- lifemate-update-v1\n$proof\n-->").put("assets",JSONArray().put(asset))
    }
    private fun parse(json: JSONObject) = ReleaseMetadata.parse(json.toString(),signer,pair.public)
    @Test fun acceptsSignedOfficialMetadata() { assertEquals(info.copy(bytes=1234,sha256=sha,signerSha256=signer),parse(metadata())) }
    @Test fun rejectsMissingProofMalformedJsonAndUnpublishedRelease() {
        assertNull(parse(metadata().put("body","Unsigned release")))
        assertNull(ReleaseMetadata.parse("not JSON",signer,pair.public))
        assertNull(parse(metadata().put("draft",true)))
        assertNull(parse(metadata().put("prerelease",true)))
    }
    @Test fun rejectsBadAssetDigestTypeStateAndForeignUrl() {
        for ((field,value) in listOf("digest" to "sha256:bad", "content_type" to "text/html", "state" to "new", "browser_download_url" to "https://evil.example/app.apk")) {
            val json=metadata();json.getJSONArray("assets").getJSONObject(0).put(field,value)
            assertNull(parse(json))
        }
    }
    @Test fun rejectsAmbiguousAssetsOrProof() {
        val json=metadata();val assets=json.getJSONArray("assets");assets.put(assets.getJSONObject(0))
        assertNull(parse(json))
        val duplicate=metadata();duplicate.put("body",duplicate.getString("body")+duplicate.getString("body"));assertNull(parse(duplicate))
    }
    private class Response(private val code: Int, private val text: String="") : HttpURLConnection(URL(ReleaseInfo.API)) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun getResponseCode() = code
        override fun getInputStream() = ByteArrayInputStream(text.toByteArray())
    }
    @Test fun apiErrorsAndInvalidAssetsNeverReturnSuccess() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        for (response in listOf(Response(403),Response(429),Response(500),Response(200,"{}"))) {
            try { UpdateRepository(context) { response }.fetch(); fail("Must not treat an invalid response as up to date") }
            catch (_: IllegalStateException) { /* Expected error, not a successful result. */ }
        }
        assertNull(UpdateRepository(context) { Response(404) }.fetch())
        try { UpdateRepository(context) { throw IOException("test offline") }.fetch(); fail("Offline must throw") }
        catch (_: IOException) { /* Expected. */ }
        context.getSharedPreferences("release_updates",0).edit().clear().commit()
        Unit
    }
}
