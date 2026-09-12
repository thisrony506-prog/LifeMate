package com.lifemate.updates

import org.junit.Assert.*
import org.junit.Test

class ReleaseInfoTest {
    private val base = "https://github.com/thisrony506-prog/LifeMate/releases/download/"
    @Test fun newerVersionIsDetectedNumerically() {
        val r = ReleaseInfo.validated("v1.2.120", "LifeMate-100120.apk", "${base}v1.2.120/LifeMate-100120.apk")!!
        assertEquals("1.2.120", r.version)
        assertTrue(r.newerThan(100119)); assertFalse(r.newerThan(100120)); assertFalse(r.newerThan(100121))
    }
    @Test fun legacyUnsignedOrDebugAssetsAreNotUpdates() {
        assertNull(ReleaseInfo.validated("v1.1.1", "LifeMate-release-unsigned.apk", "${base}v1.1.1/LifeMate-release-unsigned.apk"))
        assertNull(ReleaseInfo.validated("v1.2.1", "app-debug.apk", "${base}v1.2.1/app-debug.apk"))
    }
    @Test fun rejectsForeignHostsRepositoriesAndSchemes() {
        val url = "${base}v1.2.1/LifeMate-100001.apk"
        for (bad in listOf(url.replace("https:", "http:"), url.replace("github.com", "evil.example"), url.replace("thisrony506-prog", "other"), url + "?redirect=evil"))
            assertNull(ReleaseInfo.validated("v1.2.1", "LifeMate-100001.apk", bad))
    }
    @Test fun rejectsMismatchingCodeAndMalformedTags() {
        assertNull(ReleaseInfo.validated("v1.2.2", "LifeMate-100001.apk", "${base}v1.2.2/LifeMate-100001.apk"))
        for (tag in listOf("v1.2.01", "v1.2.0", "v1.2.-1", "v1.2.99999999999", "v1.2.1-beta", "../v1.2.1"))
            assertNull(ReleaseInfo.validated(tag, "LifeMate-100001.apk", "${base}$tag/LifeMate-100001.apk"))
    }
}
