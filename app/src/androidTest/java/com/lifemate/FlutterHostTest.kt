package com.lifemate

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test

class FlutterHostTest {
    @Test fun onlyFlutterTabsRenderAndNewProfileSurvivesActivityRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        java.io.File(context.noBackupFilesDir, com.lifemate.reset.FreshStartReset.MARKER).delete()
        com.lifemate.reset.FreshStartReset.run(context)
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            fun locate(label: String): UiObject2 {
                repeat(24) {
                    (device.findObject(By.textContains(label)) ?: device.findObject(By.descContains(label)))?.let { return it }
                    SystemClock.sleep(500)
                }
                error("Flutter did not expose $label")
            }
            locate("Continue").click()
            val field = device.wait(Until.findObject(By.clazz("android.widget.EditText")),10000)
            assertNotNull(field); field!!.text = "FreshStartProof"
            device.executeShellCommand("input keyevent 111") // ESC hides the IME without navigating back.
            for (attempt in 0..3) {
                val button = device.findObject(By.textContains("Organize my day")) ?: device.findObject(By.descContains("Organize my day"))
                if (button != null) { button.click(); break }
                device.swipe(500,1200,500,400,20)
            }
            locate("FreshStartProof")
            locate("Money").click()
            locate("Income")
            assertFalse(device.hasObject(By.textContains("All retained tools")))
            scenario.recreate()
            locate("FreshStartProof")
            locate("My Life")
        }
    }
}
