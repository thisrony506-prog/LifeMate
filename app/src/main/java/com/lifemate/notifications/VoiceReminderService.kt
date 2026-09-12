package com.lifemate.notifications

import android.app.*
import android.content.*
import android.media.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lifemate.LifeMateApp
import com.lifemate.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Locale

/** Opt-in, bounded speech only. Never listens, records, starts at boot, or bypasses quiet settings. */
class VoiceReminderService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var focus: AudioFocusRequest? = null
    private var started = false
    private val deadline = Runnable { stopSelf() }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        if (started) return START_NOT_STICKY // simultaneous alarms still retain their normal notifications
        started = true
        val text = intent?.getStringExtra("speech")?.take(240).orEmpty()
        if (text.isBlank()) { stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Spoken reminder playback",NotificationManager.IMPORTANCE_LOW).apply { setSound(null,null); enableVibration(false); lockscreenVisibility = Notification.VISIBILITY_PRIVATE })
        val stop = PendingIntent.getService(this,0,Intent(this,VoiceReminderService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            startForeground(ID,NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("LifeMate voice reminder").setContentText("Reading one reminder aloud. Tap Stop to end.")
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOngoing(true).addAction(R.drawable.ic_notification,"Stop",stop).build())
        } catch (_: Exception) { stopSelf(); return START_NOT_STICKY }
        handler.postDelayed(deadline,30_000)
        scope.launch {
          try {
            val app = application as LifeMateApp
            val prefs = app.preferences.flow.first()
            if (!prefs.voiceReminders || !prefs.notifications || !permitted(this@VoiceReminderService)) { stopSelf(); return@launch }
            val audio = getSystemService(AudioManager::class.java)
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { change -> if (change < 0) stopSelf() }.build()
            focus = request
            if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { stopSelf(); return@launch }
            tts = TextToSpeech(this@VoiceReminderService) { status -> handler.post {
                try {
                val engine = tts
                if (status != TextToSpeech.SUCCESS || engine == null || !permitted(this@VoiceReminderService)) { stopSelf(); return@post }
                val language = if (text.any { it in '\u0980'..'\u09ff' }) "bn" else "en"
                val voice = engine.voices?.filter { !it.isNetworkConnectionRequired && it.locale.language == language && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                    ?.sortedByDescending { it.locale == Locale.getDefault() }?.firstOrNull()
                if (voice == null || engine.setVoice(voice) == TextToSpeech.ERROR) { stopSelf(); return@post }
                engine.setAudioAttributes(attributes)
                engine.setSpeechRate(.95f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { if (!permitted(this@VoiceReminderService)) handler.post { stopSelf() } }
                    override fun onDone(utteranceId: String?) { handler.post { stopSelf() } }
                    @Deprecated("Required by Android") override fun onError(utteranceId: String?) { handler.post { stopSelf() } }
                })
                if (engine.speak(text,TextToSpeech.QUEUE_FLUSH,null,"reminder") == TextToSpeech.ERROR) stopSelf()
                } catch (_: Exception) { stopSelf() }
            } }
          } catch (e: CancellationException) { throw e }
          catch (_: Exception) { stopSelf() }
        }
        handler.post(object : Runnable {
            override fun run() {
                if (!permitted(this@VoiceReminderService)) stopSelf()
                else handler.postDelayed(this, 1000)
            }
        })
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); scope.cancel()
        tts?.stop(); tts?.shutdown(); tts = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    companion object {
        private const val CHANNEL = "lifemate_voice_playback"
        private const val ID = 8401
        private const val STOP = "com.lifemate.STOP_VOICE"
        fun permitted(context: Context): Boolean {
            val audio = context.getSystemService(AudioManager::class.java)
            val notifications = context.getSystemService(NotificationManager::class.java)
            return audio.ringerMode == AudioManager.RINGER_MODE_NORMAL && audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION) > 0 &&
                notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL &&
                !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked &&
                !(context.applicationContext as LifeMateApp).secure.hasPin() && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        fun speak(context: Context, text: String) {
            if (!permitted(context)) return
            // Android may deny a background foreground-service start for approximate alarms.
            // Keep the ordinary reminder instead of bypassing the restriction or retrying in a loop.
            try { ContextCompat.startForegroundService(context,Intent(context,VoiceReminderService::class.java).putExtra("speech",text)) }
            catch (_: Exception) { /* Notification remains the reliable fallback. */ }
        }
    }
}
