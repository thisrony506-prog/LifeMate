package com.lifemate

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.lifemate.database.*
import com.lifemate.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Fixture data exists only in the test APK, never in the shipping app. */
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun captureRealComposeHomeInBothThemes() {
        val app = compose.activity.application as LifeMateApp
        val today = LocalDate.now()
        runBlocking {
            app.db.dao().clear()
            app.db.dao().saveProfile(Profile(fullName = "Rony", preferredName = "Rony"))
            val morning = LifeItem(title = "Morning exercise", time = "07:00", repeat = Repeat.DAILY, notifications = false)
            val reading = LifeItem(title = "A little time to read", time = "09:00", repeat = Repeat.DAILY, notifications = false)
            val study = LifeItem(title = "Learn something new", time = "19:00", repeat = Repeat.DAILY, notifications = false)
            listOf(morning, reading, study).forEach { app.db.dao().save(it) }
            listOf(morning, reading).forEach { app.db.dao().complete(Completion(it.id, today.toString())) }
            app.db.dao().save(LifeItem(kind = Kind.BIRTHDAY, title = "Rahim", date = today.plusDays(4).toString(), notifications = false))
            val mission = LifeItem(kind = Kind.MISSION, title = "30 days of mindful living", duration = 30, date = today.minusDays(23).toString(), notifications = false)
            app.db.dao().save(mission)
            (0L..22L).forEach { app.db.dao().complete(Completion(mission.id, today.minusDays(it + 1).toString())) }
            app.preferences.set("theme", "Light")
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Search everything").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("theme-Light").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        capture("home-light.jpg")
        runBlocking { app.preferences.set("theme", "Dark") }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("theme-Dark").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        capture("home-dark.jpg")
        runBlocking { app.preferences.set("theme", "Light") }
    }
    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val scaled = Bitmap.createScaledBitmap(bitmap, 440, (bitmap.height * 440f / bitmap.width).toInt(), true)
        val dir = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, name).outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 78, it) }
    }
}
