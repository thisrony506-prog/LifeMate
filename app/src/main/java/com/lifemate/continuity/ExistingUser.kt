package com.lifemate.continuity

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** Compatibility reader, not an old feature route. Never creates/replaces a data key
 * or changes a database. In particular, corruption must NEVER trigger SQLCipher's
 * default destructive recovery handler. All work is called from Dispatchers.IO.
 */
class ExistingUser(private val context: Context) {
    private val prefs get() = context.getSharedPreferences("lifemate_secrets", Context.MODE_PRIVATE)
    private val settings by lazy {
        PreferenceDataStoreFactory.create { File(context.filesDir, "datastore/settings.preferences_pb") }
    }
    suspend fun status(): Map<String, Any> {
        val file = File(context.filesDir, "datastore/settings.preferences_pb")
        val saved = if (file.isFile) settings.data.first() else null
        return mapOf("present" to context.getDatabasePath("lifemate.db").isFile,
            "pin" to prefs.contains("pin"),
            "biometric" to (saved?.get(booleanPreferencesKey("biometric")) ?: false),
            "theme" to (saved?.get(stringPreferencesKey("theme")) ?: "System"))
    }
    private fun decrypt(name: String): ByteArray {
        val raw = requireNotNull(prefs.getString(name, null)) { "Existing secret unavailable" }
        val bytes = Base64.decode(raw, Base64.NO_WRAP)
        require(bytes.size >= 28)
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = requireNotNull(store.getKey("lifemate_v1", null) as? SecretKey)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            doFinal(bytes.copyOfRange(12, bytes.size))
        }
    }
    @Synchronized fun verifyPin(pin: String): Boolean {
        if (!prefs.contains("pin")) return true
        if (prefs.getLong("retryAt", 0) > System.currentTimeMillis()) return false
        val stored = decrypt("pin")
        try {
            require(stored.size == 48)
            val validFormat = pin.length in 6..12 && pin.all { it in '0'..'9' }
            val hash = if (validFormat) SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(PBEKeySpec(pin.toCharArray(), stored.copyOfRange(0,16), 120000, 256)).encoded else byteArrayOf()
            val valid = validFormat && MessageDigest.isEqual(stored.copyOfRange(16,48), hash)
            hash.fill(0)
            if (valid) prefs.edit().remove("attempts").remove("retryAt").commit()
            else {
                val attempts = prefs.getInt("attempts", 0).coerceAtMost(1000) + 1
                prefs.edit().putInt("attempts", attempts).putLong("retryAt", if (attempts >= 5) System.currentTimeMillis()+60000 else 0).commit()
            }
            return valid
        } finally { stored.fill(0) }
    }
    fun profile(): Map<String, Any> {
        val file = context.getDatabasePath("lifemate.db")
        if (!file.isFile) return emptyMap()
        System.loadLibrary("sqlcipher")
        val password = decrypt("database")
        try {
            return SQLiteDatabase.openDatabase(file.path, password, null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
                { _, _ -> throw IllegalStateException("Existing database could not be read; recovery must not erase it") }, null).use { db ->
                db.rawQuery("SELECT fullName,nickname,preferredName,birthday,photo,introduction,information FROM profiles WHERE id=1 LIMIT 1", emptyArray<String>()).use cursorRead@ { cursor ->
                    if (!cursor.moveToFirst()) return@cursorRead emptyMap<String, Any>()
                    val data = cursor.columnNames.associateWith { cursor.getString(cursor.getColumnIndexOrThrow(it)) ?: "" }.toMutableMap<String, Any>()
                    val path = data.remove("photo") as String
                    if (path.isNotBlank()) {
                        val photo = File(path).canonicalFile
                        require(photo.parentFile == File(context.filesDir,"media").canonicalFile) { "Photo outside original private media" }
                        if (photo.isFile) {
                            require(photo.length() <= 20 * 1024 * 1024)
                            data["photoBytes"] = photo.readBytes()
                        }
                    }
                    data
                }
            }
        } finally { password.fill(0) }
    }
}
