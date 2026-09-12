package com.lifemate.updates

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class ReleaseProofTest {
    companion object {
        // Ephemeral, in-memory TEST key pair; not an APK signing identity or stored credential.
        private val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        private val info = ReleaseInfo(100007, "1.2.7", "https://github.com/thisrony506-prog/LifeMate/releases/download/v1.2.7/LifeMate-100007.apk")
        private val sha = "ab".repeat(32)
        private val signer = "cd".repeat(32)
    }
    private fun proof(): String = Base64.getEncoder().encodeToString(Signature.getInstance("SHA256withRSA").run {
        initSign(pair.private); update(ReleaseProof.canonical(info, 1234, sha, signer)); sign()
    })
    @Test fun canonicalProtocolIsStable() {
        assertEquals("LifeMate-Update-V1\n100007\n1.2.7\n${info.downloadUrl}\n1234\n$sha\n$signer\n", String(ReleaseProof.canonical(info,1234,sha,signer),Charsets.UTF_8))
    }
    @Test fun acceptsValidProofFromInstalledSigner() {
        assertTrue(ReleaseProof.verify(info,1234,sha,signer,proof(),signer,pair.public))
    }
    @Test fun rejectsMissingCorruptOrOversizedSignature() {
        for (s in listOf("", "not-base64!", "a".repeat(2049)))
            assertFalse(ReleaseProof.verify(info,1234,sha,signer,s,signer,pair.public))
    }
    @Test fun rejectsForgedMetadataAndChangedSigner() {
        val p = proof()
        assertFalse(ReleaseProof.verify(info,1235,sha,signer,p,signer,pair.public))
        assertFalse(ReleaseProof.verify(info,1234,"ef".repeat(32),signer,p,signer,pair.public))
        assertFalse(ReleaseProof.verify(info.copy(code=100008),1234,sha,signer,p,signer,pair.public))
        assertFalse(ReleaseProof.verify(info,1234,sha,signer,p,"00".repeat(32),pair.public))
    }
    @Test fun rejectsWrongPublicKey() {
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertFalse(ReleaseProof.verify(info,1234,sha,signer,proof(),signer,other.public))
    }
    @Test fun rejectsBadSizeAndDigest() {
        assertFalse(ReleaseProof.verify(info,0,sha,signer,proof(),signer,pair.public))
        assertFalse(ReleaseProof.verify(info,209715201,sha,signer,proof(),signer,pair.public))
        assertFalse(ReleaseProof.verify(info,1234,"bad",signer,proof(),signer,pair.public))
    }
    @Test fun automaticChecksWaitSixHours() {
        assertTrue(UpdateCadence.due(100,0))
        assertFalse(UpdateCadence.due(1000,1000))
        assertFalse(UpdateCadence.due(1000+UpdateCadence.INTERVAL_MILLIS-1,1000))
        assertTrue(UpdateCadence.due(1000+UpdateCadence.INTERVAL_MILLIS,1000))
    }
    @Test fun clockRollbackDoesNotTriggerRepeatedChecks() {
        assertFalse(UpdateCadence.due(500,1000))
    }
}
