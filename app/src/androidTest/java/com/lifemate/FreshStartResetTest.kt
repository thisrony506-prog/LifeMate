package com.lifemate

import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.reset.FreshStartReset
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FreshStartResetTest {
    @Test fun oldPrivateDataIsRemovedExactlyOnceWhileUpdateCacheSurvives() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = File(context.noBackupFilesDir, FreshStartReset.MARKER)
        marker.delete()
        val old = context.getDatabasePath("lifemate.db")
        old.parentFile!!.mkdirs(); old.writeText("old encrypted fixture")
        File(context.filesDir, "old-private-note").writeText("private fixture")
        context.getSharedPreferences("release_updates",0).edit().putString("reset-test", "keep").commit()
        assertTrue(FreshStartReset.run(context))
        assertFalse(old.exists())
        assertFalse(File(context.filesDir, "old-private-note").exists())
        assertEquals("keep", context.getSharedPreferences("release_updates",0).getString("reset-test", null))
        val current = File(context.filesDir, "new-private-note").apply { writeText("new data") }
        assertFalse(FreshStartReset.run(context))
        assertEquals("new data", current.readText())
        current.delete()
        context.getSharedPreferences("release_updates",0).edit().remove("reset-test").commit()
    }
}
