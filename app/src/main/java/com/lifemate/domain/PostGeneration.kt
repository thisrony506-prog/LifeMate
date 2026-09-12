package com.lifemate.domain

import com.lifemate.database.SocialPost
import java.net.URI

/** Intentionally text-only. Photo paths, profile data, private notes and device IDs are never in this request. */
data class PostGenerationRequest(val type: String, val topic: String, val message: String, val name: String, val date: String, val tone: String)
interface PostGenerator { suspend fun generate(request: PostGenerationRequest): String }
object PostAiConfig {
    fun validEndpoint(text: String): Boolean = try {
        val uri=URI(text); val host=uri.host.orEmpty()
        uri.scheme=="https" && uri.rawUserInfo==null && uri.rawQuery==null && uri.rawFragment==null && uri.port in setOf(-1,443) &&
            host.contains('.') && !host.endsWith(".local") && !host.endsWith(".localhost") && host.any { it.isLetter() } && !host.contains(':')
    } catch (_: Exception) { false }
}
fun SocialPost.aiRequest(tone: String) = PostGenerationRequest(type,topic,message,person,date,tone)
