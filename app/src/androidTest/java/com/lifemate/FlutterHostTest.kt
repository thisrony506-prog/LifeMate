package com.lifemate

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.lifemate.database.Profile
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Exercises the real embedded Flutter engine, not the legacy Compose test route. */
class FlutterHostTest {
    @Test fun flutterTabsLoadOverTheExistingEncryptedProfile() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val app=context.applicationContext as LifeMateApp
        app.secure.removePin()
        runBlocking { app.db.dao().saveProfile(Profile(fullName="FlutterProof")) }
        ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("flutter",true)).use {
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue("Flutter home did not become accessible", device.wait(Until.hasObject(By.textContains("My Life")),30000) || device.wait(Until.hasObject(By.descContains("My Life")),5000))
            val money=device.findObject(By.textContains("Money")) ?: device.findObject(By.descContains("Money"))
            assertNotNull("Money tab missing",money);money!!.click()
            assertTrue("Flutter money page did not open",device.wait(Until.hasObject(By.textContains("Money, made simple")),10000) || device.wait(Until.hasObject(By.descContains("Money, made simple")),3000))
            assertEquals("FlutterProof",runBlocking {app.db.dao().getProfile()?.fullName})
        }
    }
}
