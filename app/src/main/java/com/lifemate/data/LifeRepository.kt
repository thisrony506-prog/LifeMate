package com.lifemate.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.lifemate.database.*
import com.lifemate.domain.*
import com.lifemate.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class LifeRepository(val context: Context, val db: LifeDatabase, private val scheduler: ReminderScheduler, val preferences: PreferenceStore) {
    val dao = db.dao()
    val mediaDir get() = File(context.filesDir, "media").apply { mkdirs() }
    suspend fun save(item: LifeItem) {
        require(item.title.isNotBlank()) { "Please add a name." }
        require(item.title.length <= 200) { "Keep the name under 200 characters." }
        LocalDate.parse(item.date); LocalTime.parse(item.time)
        require(item.duration in 1..36500 && item.interval in 1..1461)
        require(item.repeat != Repeat.SPECIFIC || item.spec().days.isNotEmpty()) { "Select at least one weekday." }
        val saved = item.copy(updatedAt = System.currentTimeMillis())
        dao.save(saved)
        scheduler.schedule(saved)
    }
    suspend fun toggle(item: LifeItem, date: LocalDate = LocalDate.now()): Boolean {
        require(date <= LocalDate.now()) { "Future days can't be completed yet." }
        val completed = db.withTransaction {
            val current = requireNotNull(dao.get(item.id)) { "This record has been deleted." }
            require(current.occurs(date)) { "This item isn't scheduled for that day." }
            if (dao.isDone(item.id, date.toString())) { dao.uncomplete(item.id, date.toString()); false }
            else { dao.complete(Completion(item.id, date.toString())); true }
        }
        if (completed && date == LocalDate.now()) scheduler.dismiss(item.id)
        scheduler.schedule(item)
        return completed
    }
    suspend fun delete(item: LifeItem) = withContext(Dispatchers.IO) {
        val attachments = dao.attachmentsFor(item.id)
        scheduler.cancel(item.id); dao.delete(item.id)
        attachments.forEach { safeMedia(it.path)?.delete() }
    }
    fun safeMedia(path: String): File? = File(path).canonicalFile.takeIf { it.parentFile == mediaDir.canonicalFile }
    suspend fun copyMedia(uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        require(mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/")) { "Choose an image, video, or audio file." }
        val file = File(mediaDir, UUID.randomUUID().toString())
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open this file." }
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024); var total = 0L
                    while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 250L * 1024 * 1024) { "Choose a file smaller than 250 MB." }; output.write(buffer, 0, n) }
                }
            }
            file.absolutePath to mime
        } catch (e: Exception) { file.delete(); throw e }
    }
    suspend fun attach(itemId: String, uri: Uri) {
        val (path, mime) = copyMedia(uri)
        try { dao.saveAttachment(Attachment(itemId = itemId, path = path, mime = mime, name = "${mime.substringBefore('/').replaceFirstChar(Char::uppercase)} · ${LocalDate.now()}")) }
        catch (e: Exception) { File(path).delete(); throw e }
    }
    suspend fun deleteAttachment(attachment: Attachment) = withContext(Dispatchers.IO) { dao.deleteAttachment(attachment.id); safeMedia(attachment.path)?.delete(); Unit }
    suspend fun savePost(post: SocialPost) {
        com.lifemate.domain.PostTemplates.validate(post)
        val previous=dao.getPost(post.id)
        dao.savePost(post.copy(updatedAt=System.currentTimeMillis()))
        if(previous!=null && previous.photo.isNotBlank() && previous.photo!=post.photo && dao.allPosts().none { it.photo==previous.photo }) safeMedia(previous.photo)?.delete()
    }
    suspend fun deletePost(post: SocialPost) = withContext(Dispatchers.IO) {
        val current=dao.getPost(post.id)
        dao.deletePost(post.id)
        if (current!=null && current.photo.isNotBlank() && dao.allPosts().none { it.photo==current.photo }) safeMedia(current.photo)?.delete()
        Unit
    }
    suspend fun postPhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val normalized = com.lifemate.utils.PostCardRenderer.importPhoto(context,uri)
        val target = File(mediaDir,"${UUID.randomUUID()}.jpg")
        try { normalized.copyTo(target); target.path } finally { normalized.delete() }
    }
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val application=context.applicationContext as com.lifemate.LifeMateApp
        if(application.clearFlutterData!=null) application.clearFlutterData!!.invoke()
        else {
            java.io.File(context.filesDir,"life_mate_v1").deleteRecursively()
            context.filesDir.listFiles()?.filter { it.name.matches(Regex("[a-f0-9-]{36}\\.encrypted")) }?.forEach { it.delete() }
        }
        dao.allItems().forEach { scheduler.cancel(it.id) }
        db.withTransaction { dao.clear() }
        mediaDir.listFiles()?.forEach { it.delete() }
        File(context.cacheDir, "cards").deleteRecursively()
        File(context.filesDir, "studio").deleteRecursively()
        com.lifemate.utils.SecureStore(context).clearStudioDrafts()
        preferences.clear()
    }
}
