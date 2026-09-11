package com.lifemate.domain

/** Extension boundary for a future consent-gated backend. The shipping app uses offline templates only. */
data class WishRequest(val name: String, val relationship: String, val tone: String)
interface AssistantProvider {
    val requiresNetwork: Boolean
    suspend fun birthdayWish(request: WishRequest): String
}
class OfflineAssistantProvider : AssistantProvider {
    override val requiresNetwork = false
    override suspend fun birthdayWish(request: WishRequest) = com.lifemate.utils.Wishes.generate(request.name, request.relationship, request.tone)
}
