package com.lifemate

import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.database.*
import com.lifemate.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.*
import java.time.ZoneId
import java.time.LocalDate
import java.time.LocalTime
import com.lifemate.domain.Schedule
import java.time.LocalDateTime

/** Opt-in seed stage for scripts/device-smoke.sh, which asserts delivery AFTER this process exits. */
class ExternalAlarmTest {
    @Test fun seedPendingAlarmForExternalVerification() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("externalAlarmTest") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LifeMateApp
        if (app.db.dao().getProfile() == null) app.db.dao().saveProfile(Profile(fullName = "Alarm verification"))
        app.preferences.set("notifications", true)
        if (args.getString("verifyPrevious") == "true") {
            val previous = requireNotNull(app.db.dao().get("5f2e5e7a-73d7-4519-a0c0-cf999de4caa0"))
            val plan = requireNotNull(app.db.dao().getAlarm(previous.id))
            val posted = app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.firstOrNull { it.tag == previous.id }
            assertNotNull("A real notification must still be present", posted)
            assertEquals("New recurrences must be allowed to alert", 0, posted!!.notification.flags and android.app.Notification.FLAG_ONLY_ALERT_ONCE)
            val originalTime = LocalDate.parse(previous.date).atTime(LocalTime.parse(previous.time)).atZone(ZoneId.systemDefault()).toInstant()
            assertEquals(Schedule.next(previous.spec(), originalTime, ZoneId.systemDefault())!!.toEpochMilli(), plan.occurrence)
        }
        if (args.getString("verifyOnly") == "true") return@runBlocking
        val trigger = LocalDateTime.now().plusSeconds(25).withNano(0)
        val item = LifeItem(id = "5f2e5e7a-73d7-4519-a0c0-cf999de4caa0", kind = Kind.REMINDER,
            title = args.getString("alarmTitle") ?: "LifeMate delivery check", date = trigger.toLocalDate().toString(),
            time = trigger.toLocalTime().toString(), repeat = Repeat.DAILY, sound = "silent", vibration = false)
        app.scheduler.cancel(item.id)
        app.repository.save(item)
    }
}
