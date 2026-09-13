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
                repeat(60) {
                    (device.findObject(By.textContains(label).pkg(context.packageName)) ?: device.findObject(By.descContains(label).pkg(context.packageName)))?.let { return it }
                    SystemClock.sleep(500)
                }
                val bytes = java.io.ByteArrayOutputStream()
                device.dumpWindowHierarchy(bytes)
                val xml = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(java.io.ByteArrayInputStream(bytes.toByteArray()))
                val nodes = xml.getElementsByTagName("node")
                val visible = (0 until nodes.length).map { nodes.item(it) as org.w3c.dom.Element }.map { it.getAttribute("text") + " " + it.getAttribute("content-desc") }.filter { it.isNotBlank() }.joinToString(" | ")
                error("Flutter did not expose $label. Visible test labels: ${visible.take(2400)}")
            }
            locate("Continue").click()
            val field = device.wait(Until.findObject(By.clazz("android.widget.EditText").pkg(context.packageName)),10000)
            assertNotNull(field)
            field!!.click()
            // Exercise the real Flutter text-input connection, not only an
            // accessibility node's cached ACTION_SET_TEXT value.
            device.executeShellCommand("input text FreshStartProof")
            locate("FreshStartProof")
            scenario.onActivity { activity ->
                androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
            }
            SystemClock.sleep(300)
            for (attempt in 0..3) {
                val button = device.findObject(By.textContains("Organize my day")) ?: device.findObject(By.descContains("Organize my day"))
                if (button != null) { button.click(); break }
                device.swipe(500,1200,500,400,20)
            }
            locate("Money") // Wait for Home, not the outgoing onboarding text field.
            locate("FreshStartProof")
            locate("Money").click()
            locate("Income")
            assertFalse(device.hasObject(By.textContains("All retained tools")))
            scenario.recreate()
            // Flutter may retain the selected Money tab across recreation.
            locate("Home").click()
            locate("FreshStartProof")
            locate("My Life")
        }
    }
}
