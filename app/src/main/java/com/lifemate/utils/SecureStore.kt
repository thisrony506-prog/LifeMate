package com.lifemate.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** Device-bound secrets, encrypted using a non-exportable Android Keystore key. */
class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("lifemate_secrets", Context.MODE_PRIVATE)
    private val key: javax.crypto.SecretKey
        get() {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            return (store.getKey("lifemate_v1", null) as? javax.crypto.SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("lifemate_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }
    @Synchronized private fun put(name: String, value: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        check(prefs.edit().putString(name, Base64.encodeToString(cipher.iv + cipher.doFinal(value), Base64.NO_WRAP)).commit())
    }
    @Synchronized private fun get(name: String): ByteArray? {
        val raw = prefs.getString(name, null) ?: return null
        val bytes = Base64.decode(raw, Base64.NO_WRAP)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            doFinal(bytes.copyOfRange(12, bytes.size))
        }
    }
    @Synchronized fun databaseKey(): ByteArray = get("database") ?: ByteArray(32).apply { SecureRandom().nextBytes(this); put("database", this) }
    fun readStudioDraft(id: String): String? = get("studio:$id")?.toString(Charsets.UTF_8)
    fun saveStudioDraft(id: String, json: String) = put("studio:$id", json.toByteArray(Charsets.UTF_8))
    fun clearStudioDrafts() { val edit = prefs.edit(); prefs.all.keys.filter { it.startsWith("studio:") }.forEach { edit.remove(it) }; edit.commit() }
    fun hasPin() = prefs.contains("pin")
    fun setPin(pin: String) {
        require(pin.length in 6..12 && pin.all(Char::isDigit))
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        put("pin", salt + hash(pin, salt)); resetAttempts()
    }
    fun retrySeconds(): Long = ((prefs.getLong("retryAt", 0) - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    fun verifyPin(pin: String): Boolean {
        if (retrySeconds() > 0) return false
        val stored = get("pin") ?: return true
        val valid = MessageDigest.isEqual(stored.copyOfRange(16, stored.size), hash(pin, stored.copyOfRange(0, 16)))
        if (valid) resetAttempts() else {
            val attempts = prefs.getInt("attempts", 0) + 1
            prefs.edit().putInt("attempts", attempts).putLong("retryAt", if (attempts >= 5) System.currentTimeMillis() + 60_000 else 0).commit()
        }
        return valid
    }
    fun removePin() { prefs.edit().remove("pin").commit(); resetAttempts() }
    private fun resetAttempts() { prefs.edit().remove("attempts").remove("retryAt").commit() }
    private fun hash(pin: String, salt: ByteArray) = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)).encoded
}
