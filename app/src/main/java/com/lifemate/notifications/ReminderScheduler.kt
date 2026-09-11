package com.lifemate.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.lifemate.*
import com.lifemate.data.PreferenceStore
import com.lifemate.database.*
import com.lifemate.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context, private val dao: LifeDao, private val preferences: PreferenceStore) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()
    fun canBePrecise() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    fun permissionGranted() = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    private fun pending(id: String, occurrence: Long = 0): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, AlarmReceiver::class.java).setData(Uri.parse("lifemate://alarm/$id"))
            .putExtra("id", id).putExtra("occurrence", occurrence), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun cancel(id: String) { alarms.cancel(pending(id)); NotificationManagerCompat.from(context).cancel(id.hashCode()) }
    suspend fun schedule(item: LifeItem) = mutex.withLock { scheduleInternal(item) }
    private suspend fun scheduleInternal(item: LifeItem) {
        cancel(item.id)
        val prefs = preferences.flow.first()
        if (item.archived || !item.notifications || !prefs.notifications) return
        var next = Schedule.next(item.spec(), Instant.now(), ZoneId.systemDefault()) ?: return
        // Do not wake the device for a daily target already completed.
        if (dao.isDone(item.id, next.atZone(ZoneId.systemDefault()).toLocalDate().toString())) {
            next = Schedule.next(item.spec(), next, ZoneId.systemDefault()) ?: return
        }
        val intent = pending(item.id, next.toEpochMilli())
        try {
            if (canBePrecise()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), intent)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), intent)
        }
    }
    suspend fun reconcile() {
        dao.allItems().forEach { schedule(it) }
        dao.pruneDeliveries(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90))
    }
    suspend fun deliver(id: String, occurrence: Long) = mutex.withLock {
        val item = dao.get(id) ?: return@withLock
        val prefs = preferences.flow.first()
        val day = Instant.ofEpochMilli(occurrence).atZone(ZoneId.systemDefault()).toLocalDate()
        // Ignore stale broadcasts (for example after a delayed restore) and completed records.
        val timely = occurrence > 0 && kotlin.math.abs(System.currentTimeMillis() - occurrence) < TimeUnit.HOURS.toMillis(12)
        if (timely && prefs.notifications && item.notifications && !item.archived && permissionGranted() && !dao.isDone(id, day.toString())) {
            val sound = if (item.sound == "default") prefs.sound else item.sound
            val vibration = prefs.vibration && item.vibration
            val channelId = "reminders_${sound.hashCode()}_${vibration}_${item.important}"
            val channel = NotificationChannel(channelId, if (sound == "silent") "Silent reminders" else "LifeMate reminders", if (item.important) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Personal reminders. Sound and vibration can be changed in Android settings."
                enableVibration(vibration)
                setSound(if (sound == "silent") null else if (sound.startsWith("content:")) Uri.parse(sound) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            val name = dao.getProfile()?.displayName ?: "friend"
            val message = item.notificationText.ifBlank {
                when (item.kind) {
                    Kind.ROUTINE -> "Hello, $name. It's time for your ${item.title}."
                    Kind.MISSION -> "Day ${(ChronoUnit.DAYS.between(LocalDate.parse(item.date), day) + 1).coerceIn(1, item.duration.toLong())} of ${item.title} is waiting."
                    Kind.BIRTHDAY -> {
                        val away = ChronoUnit.DAYS.between(day, Schedule.nextBirthday(LocalDate.parse(item.date), day))
                        if (away == 0L) "🎂 ${item.title}'s birthday is today." else "${item.title}'s birthday is in $away days."
                    }
                    else -> "${item.title} is waiting. A little progress goes a long way."
                }
            }
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
                .setData(Uri.parse("lifemate://item/$id")).putExtra("itemId", id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(item.title).setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(open).setAutoCancel(true).setCategory(if (item.kind == Kind.BIRTHDAY) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOnlyAlertOnce(true).build()
            if (dao.recordDelivery(Delivery(id, occurrence)) != -1L) {
                try { NotificationManagerCompat.from(context).notify(id.hashCode(), notification) } catch (_: SecurityException) { /* Revoked between check and delivery. */ }
            }
        }
        // Schedule without cancelling the notification that was just delivered.
        val next = Schedule.next(item.spec(), Instant.now().plusSeconds(1), ZoneId.systemDefault())
        if (next != null && !item.archived && item.notifications && prefs.notifications) {
            try {
                if (canBePrecise()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pending(id, next.toEpochMilli()))
                else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pending(id, next.toEpochMilli()))
            } catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pending(id, next.toEpochMilli())) }
        }
    }
    fun enqueueMaintenance() {
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork("reminder-safety-net", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RescheduleWorker>(12, TimeUnit.HOURS).build())
        enqueueReschedule(context)
    }
    companion object {
        fun enqueueReschedule(context: Context) { WorkManager.getInstance(context).enqueueUniqueWork("reschedule", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RescheduleWorker>().build()) }
    }
}
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8_000) { (context.applicationContext as LifeMateApp).scheduler.deliver(id, intent.getLongExtra("occurrence", 0)) } }
            catch (_: Exception) { ReminderScheduler.enqueueReschedule(context) }
            finally { pending.finish() }
        }
    }
}
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) ReminderScheduler.enqueueReschedule(context)
    }
}
class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        (applicationContext as LifeMateApp).scheduler.reconcile(); Result.success()
    } catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
}
