package com.lifemate.domain

import com.lifemate.database.SocialPost
import org.junit.Assert.*
import org.junit.Test

class PostTemplatesTest {
    @Test fun allTenTypesAndTonesProduceEditableOfflineCaptions() {
        assertEquals(10,PostTemplates.types.size)
        PostTemplates.types.forEach {type-> PostTemplates.tones.forEach {tone->
            val post=SocialPost(type=type,topic="Reading",message="30 days",person="Rony",date="2026-09-13")
            val result=PostTemplates.caption(post,tone)
            assertTrue(result.contains("30 days"));assertTrue(result.contains("Rony"));assertTrue(result.length<=4000)
        } }
    }
    @Test fun graphicsHaveShortReusableHeadings() {
        assertTrue(PostTemplates.graphic(SocialPost(type="Birthday Post",person="Rahim")).startsWith("Happy Birthday"))
        assertTrue(PostTemplates.graphic(SocialPost(type="Mission Completed",message="30 Days ✓")).contains("30 Days ✓"))
        assertTrue(PostTemplates.graphic(SocialPost(type="Motivation")).contains("One Step"))
    }
    @Test fun missingOrExcessiveContentIsRejected() {
        try {PostTemplates.validate(SocialPost());fail()} catch (_:IllegalArgumentException) {}
        try {PostTemplates.validate(SocialPost(topic="x".repeat(161)));fail()} catch (_:IllegalArgumentException) {}
        PostTemplates.validate(SocialPost(caption="Valid local draft"))
    }
    @Test fun aiRequestIsExplicitAndTextOnly() {
        val post=SocialPost(topic="Topic",photo="/private/photo.jpg",caption="Private existing caption")
        val request=post.aiRequest("Short").toString()
        assertFalse(request.contains("/private"));assertFalse(request.contains("Private existing caption"))
    }
    @Test fun backendConfigurationHasNoCredentialsQueryOrCleartext() {
        assertTrue(PostAiConfig.validEndpoint("https://posts.example.com/v1/posts"))
        listOf("http://example.com","https://user:key@example.com","https://example.com?api_key=secret","https://127.0.0.1/api","https://localhost/api","https://example.com:8443/api").forEach {assertFalse(it,PostAiConfig.validEndpoint(it))}
    }
}
