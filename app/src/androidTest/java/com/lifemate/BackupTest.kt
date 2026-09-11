package com.lifemate

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.data.BackupManager
import com.lifemate.database.*
import com.lifemate.domain.Kind
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.zip.*

class BackupTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LifeMateApp
    @After fun cleanup() = runBlocking { app.repository.deleteAll() }
    @Test fun portableBackupRoundTripsRecordsAndPrivateMedia(): Unit = runBlocking {
        app.repository.deleteAll()
        val dao = app.db.dao()
        dao.saveProfile(Profile(fullName = "Backup test", information = "Private information"))
        val item = LifeItem(kind = Kind.NOTE, title = "Keep this", description = "Local text", notifications = false)
        dao.save(item); dao.complete(Completion(item.id, item.date))
        val media = File(app.repository.mediaDir, "fixture.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        dao.saveAttachment(Attachment(itemId = item.id, path = media.absolutePath, mime = "image/jpeg", name = "Test image"))
        val backup = File(app.cacheDir, "round-trip.zip")
        BackupManager(app.repository).export(Uri.fromFile(backup))
        app.repository.deleteAll()
        BackupManager(app.repository).restore(Uri.fromFile(backup))
        assertEquals("Private information", dao.getProfile()?.information)
        assertEquals("Local text", dao.get(item.id)?.description)
        assertTrue(dao.isDone(item.id, item.date))
        val attachment = dao.allAttachments().single()
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), File(attachment.path).readBytes())
        backup.delete()
    }
    @Test fun unsafeArchiveDoesNotReplaceCurrentData(): Unit = runBlocking {
        app.repository.deleteAll(); app.db.dao().saveProfile(Profile(fullName = "Keep me"))
        val backup = File(app.cacheDir, "unsafe.zip")
        ZipOutputStream(backup.outputStream()).use { zip -> zip.putNextEntry(ZipEntry("../escaped.txt")); zip.write("bad".toByteArray()); zip.closeEntry() }
        var rejected = false
        try { BackupManager(app.repository).restore(Uri.fromFile(backup)) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected); assertEquals("Keep me", app.db.dao().getProfile()?.fullName)
        assertFalse(File(app.cacheDir, "escaped.txt").exists()); backup.delete()
    }
}
