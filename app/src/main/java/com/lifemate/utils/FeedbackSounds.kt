package com.lifemate.utils

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import com.lifemate.R

/** Optional local micro-feedback, never a background service or sound on every tap. */
class FeedbackSounds(private val context: Context) {
    private var pool: SoundPool? = null
    private val ids=mutableMapOf<String,Int>()
    private val ready=java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private var last=0L
    private var stream=0
    var foreground=false
    fun prepare() {
        if(pool!=null) return
        val sound=SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        pool=sound
        sound.setOnLoadCompleteListener { _,id,status -> if(status==0) ready.add(id) }
        ids["task"]=sound.load(context,R.raw.soft_complete,1)
        ids["mission"]=sound.load(context,R.raw.soft_mission,1)
        ids["success"]=sound.load(context,R.raw.soft_success,1)
    }
    fun play(event: String, enabled: Boolean) {
        if(!enabled || !foreground || SystemClock.elapsedRealtime()-last<500) return
        val audio=context.getSystemService(AudioManager::class.java)
        if(audio.ringerMode!=AudioManager.RINGER_MODE_NORMAL || audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0 || context.getSystemService(NotificationManager::class.java).currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL) return
        val id=ids[event] ?: return
        if(id !in ready) return
        last=SystemClock.elapsedRealtime(); stream=pool?.play(id,.45f,.45f,1,0,1f) ?: 0
    }
    fun stop() { pool?.stop(stream) }
    fun close() {pool?.release();pool=null;ready.clear()}
}
