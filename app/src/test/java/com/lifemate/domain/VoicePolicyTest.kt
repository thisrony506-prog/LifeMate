package com.lifemate.domain

import org.junit.Assert.*
import org.junit.Test

class VoicePolicyTest {
    @Test fun respectsQuietPrivacyAndPermissionSettings() {
        assertTrue(VoicePolicy.allowed(true,5,true,false,false,true))
        assertFalse(VoicePolicy.allowed(false,5,true,false,false,true))
        assertFalse(VoicePolicy.allowed(true,0,true,false,false,true))
        assertFalse(VoicePolicy.allowed(true,5,false,false,false,true))
        assertFalse(VoicePolicy.allowed(true,5,true,true,false,true))
        assertFalse(VoicePolicy.allowed(true,5,true,false,true,true))
        assertFalse(VoicePolicy.allowed(true,5,true,false,false,false))
    }
}
