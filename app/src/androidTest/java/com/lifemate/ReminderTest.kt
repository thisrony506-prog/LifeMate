package com.lifemate

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.database.*
import com.lifemate.domain.*
import com.lifemate.notifications.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class ReminderTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LifeMateApp
    private fun scheduled(id: String): PendingIntent? = PendingIntent.getBroadcast(app, 0, Intent(app, AlarmReceiver::class.java).setData(Uri.parse("lifemate://alarm/$id")), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
    @Test fun alarmCanBeReconciledAfterSchedulerRecreationWithoutNotificationPermission() = runBlocking {
        app.db.dao().saveProfile(Profile(fullName = "Reminder test"))
        app.preferences.set("notifications", true)
        val item = LifeItem(title = "Tomorrow", date = LocalDate.now().plusDays(1).toString(), time = "10:00", repeat = Repeat.DAILY)
        app.repository.save(item)
        assertNotNull(scheduled(item.id))
        assertNotNull(app.db.dao().getAlarm(item.id))
        app.scheduler.cancel(item.id)
        val recreated = ReminderScheduler(app, app.db.dao(), app.preferences)
        recreated.reconcile()
        assertNotNull(scheduled(item.id))
        assertNotNull(app.db.dao().getAlarm(item.id))
        app.repository.delete(item)
        assertNull(app.db.dao().get(item.id))
    }
    @Test fun birthdayHasExpectedFirstReminderAndSurvivesReconciliation() = runBlocking {
        app.db.dao().saveProfile(Profile(fullName = "Birthday test"))
        val birthday = LocalDate.now().plusDays(10)
        val item = LifeItem(kind = Kind.BIRTHDAY, title = "Rahim", date = birthday.toString(), birthdayOffsets = "7,3,1,0")
        app.repository.save(item)
        val next = Schedule.next(item.spec(), Instant.now(), ZoneId.systemDefault())!!
        assertEquals(LocalDate.now().plusDays(3), next.atZone(ZoneId.systemDefault()).toLocalDate())
        app.scheduler.reconcile()
        assertNotNull(scheduled(item.id))
        assertNotNull(app.db.dao().getAlarm(item.id))
        app.repository.delete(item)
    }
}
