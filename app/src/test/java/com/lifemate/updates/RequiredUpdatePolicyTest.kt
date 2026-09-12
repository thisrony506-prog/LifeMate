package com.lifemate.updates

import org.junit.Assert.*
import org.junit.Test

class RequiredUpdatePolicyTest {
    private val old = ReleaseInfo(100043,"1.2.43","official-verified-old")
    private val newer = ReleaseInfo(100045,"1.2.45","official-verified-new")
    @Test fun serverRollbackCannotForgetKnownUpdate() { assertEquals(newer,retainNewestRelease(newer,old)) }
    @Test fun missingReleaseCannotForgetKnownUpdate() { assertEquals(newer,retainNewestRelease(newer,null)) }
    @Test fun noVerifiedReleaseDoesNotInventOne() { assertNull(retainNewestRelease(null,null)) }
    @Test fun genuinelyNewerReleaseReplacesCache() { assertEquals(newer,retainNewestRelease(old,newer)) }
}
