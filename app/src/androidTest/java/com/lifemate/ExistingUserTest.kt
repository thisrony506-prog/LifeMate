package com.lifemate

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.continuity.ExistingUser
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class ExistingUserTest {
    @Test fun originalEncryptedProfileKeyMediaAndPinSurviveReadWithoutMutation() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = c.getDatabasePath("lifemate.db")
        val prefs = c.getSharedPreferences("lifemate_secrets", Context.MODE_PRIVATE)
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // Fixture-only: refuse to touch any pre-existing user storage/key.
        check(!dbFile.exists() && prefs.all.isEmpty() && !keys.containsAlias("lifemate_v1"))
        val key = KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("lifemate_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes("GCM").setEncryptionPaddings("NoPadding").build())
        }.generateKey()
        fun seal(bytes: ByteArray): String {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
            return Base64.encodeToString(cipher.iv + cipher.doFinal(bytes), Base64.NO_WRAP)
        }
        val password = ByteArray(32) { (it + 17).toByte() }
        val salt = ByteArray(16) { it.toByte() }
        val pin = salt + SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec("123456".toCharArray(), salt, 120000, 256)).encoded
        val media = File(c.filesDir, "media/continuity-test-image").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1,2,3)) }
        try {
            prefs.edit().putString("database", seal(password)).putString("pin", seal(pin)).commit()
            System.loadLibrary("sqlcipher")
            dbFile.parentFile!!.mkdirs()
            SQLiteDatabase.openDatabase(dbFile.path, password, null, SQLiteDatabase.CREATE_IF_NECESSARY, null).use { db ->
                db.execSQL("CREATE TABLE profiles(id INTEGER PRIMARY KEY, fullName TEXT, nickname TEXT, preferredName TEXT, birthday TEXT, photo TEXT, introduction TEXT, information TEXT)")
                db.execSQL("INSERT INTO profiles VALUES(1,?,?,?,?,?,?,?)", arrayOf("Existing Person", "Nick", "UserProof", "2000-01-02", media.path, "Private intro", "Private information"))
            }
            val before = MessageDigest.getInstance("SHA-256").digest(dbFile.readBytes())
            val sealedKey = prefs.getString("database", null)
            val reader = ExistingUser(c)
            val profile = reader.profile()
            assertEquals("UserProof", profile["preferredName"])
            assertEquals("Private information", profile["information"])
            assertArrayEquals(byteArrayOf(1,2,3), profile["photoBytes"] as ByteArray)
            assertArrayEquals(before, MessageDigest.getInstance("SHA-256").digest(dbFile.readBytes()))
            assertEquals(sealedKey, prefs.getString("database", null))
            repeat(5) { assertFalse(reader.verifyPin("000000")) }
            assertFalse(reader.verifyPin("123456")) // Existing persisted cooldown is enforced.
            prefs.edit().putLong("retryAt", 0).commit() // Fixture clock reset, not production API.
            assertTrue(reader.verifyPin("123456"))
            assertTrue(media.exists())
            assertTrue(keys.containsAlias("lifemate_v1"))
            dbFile.writeText("corrupt-original-fixture")
            val corrupt = dbFile.readBytes()
            assertThrows(Exception::class.java) { reader.profile() }
            assertArrayEquals(corrupt, dbFile.readBytes())
            assertEquals(sealedKey, prefs.getString("database", null))
        } finally {
            c.deleteDatabase("lifemate.db")
            prefs.edit().clear().commit()
            keys.deleteEntry("lifemate_v1")
            media.delete()
        }
    }
    @Test fun missingOldKeyNeverCreatesAReplacementOrErasesDatabase() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val file = c.getDatabasePath("lifemate.db")
        check(!file.exists())
        file.parentFile!!.mkdirs(); file.writeText("unreadable-original-fixture")
        val before = file.readBytes()
        try {
            assertThrows(Exception::class.java) { ExistingUser(c).profile() }
            assertArrayEquals(before, file.readBytes())
        } finally { file.delete() }
    }
}
