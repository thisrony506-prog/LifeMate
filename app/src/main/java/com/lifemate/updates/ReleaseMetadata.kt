package com.lifemate.updates

import org.json.JSONObject
import java.security.PublicKey

/** Fail-closed parser. A filename alone is never evidence that an APK was signed. */
object ReleaseMetadata {
    fun parse(text: String, installedSigner: String, publicKey: PublicKey): ReleaseInfo? = try {
        val release = JSONObject(text)
        if (release.optBoolean("draft", true) || release.optBoolean("prerelease", true)) null
        else {
            val blocks = Regex("<!-- lifemate-update-v1\\n(\\{[^\\n]*\\})\\n-->").findAll(release.optString("body")).toList()
            if (blocks.size != 1) null else {
                val proof = JSONObject(blocks.single().groupValues[1])
                val assets = release.optJSONArray("assets")
                val valid = mutableListOf<ReleaseInfo>()
                if (assets != null) for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val info = ReleaseInfo.validated(release.optString("tag_name"), asset.optString("name"), asset.optString("browser_download_url")) ?: continue
                    val size = asset.optLong("size", -1)
                    val sha = proof.optString("sha256")
                    if (asset.optString("state") != "uploaded" ||
                        asset.optString("content_type") !in setOf("application/vnd.android.package-archive", "application/octet-stream") ||
                        asset.optString("digest") != "sha256:$sha" ||
                        proof.optLong("code", -1) != info.code.toLong() || proof.optString("version") != info.version ||
                        proof.optString("url") != info.downloadUrl || proof.optLong("size", -1) != size) continue
                    if (ReleaseProof.verify(info, size, sha, proof.optString("signer"), proof.optString("signature"), installedSigner, publicKey)) valid.add(info)
                }
                valid.singleOrNull()
            }
        }
    } catch (_: Exception) { null }
}
