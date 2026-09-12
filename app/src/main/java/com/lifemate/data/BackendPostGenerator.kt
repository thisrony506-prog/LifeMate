package com.lifemate.data

import com.lifemate.domain.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** API keys live on the owner's backend, never in this APK/preferences. Called only after per-request consent. */
class BackendPostGenerator(private val endpoint: String) : PostGenerator {
    override suspend fun generate(request: PostGenerationRequest): String = withContext(Dispatchers.IO) {
        require(PostAiConfig.validEndpoint(endpoint)) { "Configure a trusted HTTPS backend first." }
        val connection=URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod="POST"; connection.doOutput=true; connection.instanceFollowRedirects=false
            connection.connectTimeout=15000; connection.readTimeout=20000
            connection.setRequestProperty("Content-Type","application/json; charset=utf-8")
            val body=JSONObject().put("type",request.type).put("topic",request.topic.take(160)).put("message",request.message.take(600))
                .put("name",request.name.take(100)).put("date",request.date).put("tone",request.tone).toString().toByteArray()
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            check(connection.responseCode==200) { "The AI backend is unavailable. Your local draft is unchanged." }
            val output=java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer=ByteArray(4096)
                while(true) { ensureActive(); val n=input.read(buffer); if(n<0) break; check(output.size()+n<=32768) { "AI response is too large." }; output.write(buffer,0,n) }
            }
            val caption=JSONObject(output.toString("UTF-8")).getString("caption").trim()
            check(caption.isNotBlank() && caption.length<=4000) { "AI returned an invalid caption." }
            caption
        } finally { connection.disconnect() }
    }
}
