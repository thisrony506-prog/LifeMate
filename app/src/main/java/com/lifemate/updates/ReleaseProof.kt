package com.lifemate.updates

import java.nio.charset.StandardCharsets.UTF_8
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

/** Release metadata must be signed by the same retained key as this installed APK. */
object ReleaseProof {
    fun canonical(info: ReleaseInfo, size: Long, digest: String, signer: String): ByteArray =
        "LifeMate-Update-V1\n${info.code}\n${info.version}\n${info.downloadUrl}\n$size\n$digest\n$signer\n".toByteArray(UTF_8)

    fun verify(info: ReleaseInfo, size: Long, digest: String, signer: String, signature: String,
               installedSigner: String, publicKey: PublicKey): Boolean {
        if (size !in 1..209_715_200L || !Regex("[a-f0-9]{64}").matches(digest) ||
            !Regex("[a-f0-9]{64}").matches(signer) || signer != installedSigner || signature.length > 2048) return false
        return try {
            Signature.getInstance("SHA256withRSA").run {
                initVerify(publicKey)
                update(canonical(info, size, digest, signer))
                verify(Base64.getDecoder().decode(signature))
            }
        } catch (_: Exception) { false }
    }
}

object UpdateCadence {
    const val INTERVAL_MILLIS = 6 * 60 * 60 * 1000L
    // A backwards wall-clock change must not cause repeated automatic requests.
    fun due(now: Long, lastAttempt: Long) = lastAttempt == 0L || (now >= lastAttempt && now - lastAttempt >= INTERVAL_MILLIS)
}
