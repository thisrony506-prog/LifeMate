package com.lifemate.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")
data class Preferences(val theme: String = "System", val notifications: Boolean = true, val vibration: Boolean = true, val sound: String = "default", val biometric: Boolean = false, val automaticUpdates: Boolean = true, val voiceReminders: Boolean = false, val soundEffects: Boolean = false, val postAiEndpoint: String = "")
class PreferenceStore(private val context: Context) {
    val flow = context.dataStore.data.map { p -> Preferences(p[stringPreferencesKey("theme")] ?: "System", p[booleanPreferencesKey("notifications")] ?: true, p[booleanPreferencesKey("vibration")] ?: true, p[stringPreferencesKey("sound")] ?: "default", p[booleanPreferencesKey("biometric")] ?: false, p[booleanPreferencesKey("automaticUpdates")] ?: true, p[booleanPreferencesKey("voiceReminders")] ?: false, p[booleanPreferencesKey("soundEffects")] ?: false, p[stringPreferencesKey("postAiEndpoint")] ?: "") }
    suspend fun set(key: String, value: String) { context.dataStore.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun set(key: String, value: Boolean) { context.dataStore.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun clear() { context.dataStore.edit { it.clear() } }
}
