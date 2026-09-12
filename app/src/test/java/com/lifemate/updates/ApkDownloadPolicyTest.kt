package com.lifemate.updates

import org.junit.Assert.*
import org.junit.Test

class ApkDownloadPolicyTest {
    private val release = ReleaseInfo(100060,"1.2.60","https://github.com/thisrony506-prog/LifeMate/releases/download/v1.2.60/LifeMate-100060.apk",1234,"ab".repeat(32),"cd".repeat(32))
    @Test fun signedProofFieldsAndNumericVersionAreRequired() {
        assertTrue(ApkDownloadPolicy.validProof(release,100052))
        assertFalse(ApkDownloadPolicy.validProof(release,100060))
        assertFalse(ApkDownloadPolicy.validProof(release.copy(bytes=0),100052))
        assertFalse(ApkDownloadPolicy.validProof(release.copy(bytes=209715201),100052))
        assertFalse(ApkDownloadPolicy.validProof(release.copy(sha256=""),100052))
        assertFalse(ApkDownloadPolicy.validProof(release.copy(signerSha256=""),100052))
        assertFalse(ApkDownloadPolicy.validProof(release.copy(downloadUrl="https://github.com/other/app.apk"),100052))
    }
    @Test fun githubHttpsAssetRedirectsAreAllowed() {
        assertTrue(ApkDownloadPolicy.allowed(release.downloadUrl))
        assertTrue(ApkDownloadPolicy.allowed("https://release-assets.githubusercontent.com/github-production-release-asset/file?jwt=public-token"))
        assertTrue(ApkDownloadPolicy.allowed("https://objects.githubusercontent.com/asset"))
    }
    @Test fun unsafeRedirectsAreRejected() {
        listOf("http://github.com/file","https://github.com.evil.example/file","https://evil.example/file","https://user@github.com/file","https://github.com:8443/file","file:///tmp/file","https://release-assets.githubusercontent.com/file#fragment","not a url").forEach { assertFalse(it,ApkDownloadPolicy.allowed(it)) }
    }
    @Test fun progressIsBoundedAndOnlyTransfersAreBusy() {
        assertEquals(0f,ApkDownloadState(total=0).progress,0f)
        assertEquals(1f,ApkDownloadState(received=200,total=100).progress,0f)
        assertTrue(ApkDownloadState(phase=DownloadPhase.VERIFYING).busy)
        assertFalse(ApkDownloadState(phase=DownloadPhase.READY).busy)
    }
}
