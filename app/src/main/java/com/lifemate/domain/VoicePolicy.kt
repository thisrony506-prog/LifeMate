package com.lifemate.domain

object VoicePolicy {
    fun allowed(normalRinger: Boolean, volume: Int, unrestricted: Boolean, screenLocked: Boolean, appLocked: Boolean, notificationsAllowed: Boolean) =
        normalRinger && volume > 0 && unrestricted && !screenLocked && !appLocked && notificationsAllowed
}
