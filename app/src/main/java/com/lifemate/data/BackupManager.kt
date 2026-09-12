package com.lifemate.data

import android.net.Uri
import androidx.room.withTransaction
import com.lifemate.database.*
import com.lifemate.domain.*
import com.lifemate.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*
import java.io.*
import java.time.*
import java.util.UUID
import java.util.zip.*

/** User-initiated portable ZIP backups. They are not encrypted; the UI warns before export. */
class BackupManager(private val repo: LifeRepository) {
    private fun Profile.json() = JSONObject().put("fullName", fullName).put("nickname", nickname).put("preferredName", preferredName)
        .put("birthday", birthday).put("photo", File(photo).name).put("introduction", introduction).put("information", information)
    private fun LifeItem.json(): JSONObject = JSONObject().apply {
        put("id", id); put("kind", kind.name); put("title", title); put("description", description); put("date", date); put("time", time)
        put("repeat", repeat.name); put("weekdays", weekdays); put("interval", interval); put("duration", duration); put("dailyTarget", dailyTarget)
        put("progress", progress); put("checklist", checklist); put("notes", notes); put("tags", tags); put("relationship", relationship); put("nickname", nickname)
        put("birthdayOffsets", birthdayOffsets); put("notifications", notifications); put("notificationText", notificationText); put("sound", sound)
        put("vibration", vibration); put("important", important); put("pinned", pinned); put("archived", archived); put("createdAt", createdAt); put("updatedAt", updatedAt)
    }
    private fun parseItem(j: JSONObject) = LifeItem(
        id = UUID.fromString(j.getString("id")).toString(), kind = Kind.valueOf(j.getString("kind")), title = j.getString("title"),
        description = j.optString("description"), date = LocalDate.parse(j.getString("date")).toString(), time = LocalTime.parse(j.getString("time")).toString(),
        repeat = Repeat.valueOf(j.getString("repeat")), weekdays = j.optString("weekdays"), interval = j.getInt("interval"), duration = j.getInt("duration"),
        dailyTarget = j.optString("dailyTarget"), progress = j.getInt("progress"), checklist = j.optString("checklist", "[]"), notes = j.optString("notes"),
        tags = j.optString("tags"), relationship = j.optString("relationship"), nickname = j.optString("nickname"), birthdayOffsets = j.optString("birthdayOffsets", "7,3,1,0"),
        notifications = j.optBoolean("notifications", true), notificationText = j.optString("notificationText"), sound = j.optString("sound", "default"),
        vibration = j.optBoolean("vibration", true), important = j.optBoolean("important"), pinned = j.optBoolean("pinned"), archived = j.optBoolean("archived"),
        createdAt = j.getLong("createdAt"), updatedAt = j.getLong("updatedAt")
    ).also {
        require(it.title.isNotBlank() && it.title.length <= 200 && it.duration in 1..36500 && it.interval in 1..1461 && it.progress in 0..100) { "Invalid backup record." }
        require(it.spec().days.all { d -> d in 1..7 } && it.spec().birthdayOffsets.all { d -> d in 0..366 })
        require(it.repeat != Repeat.SPECIFIC || it.spec().days.isNotEmpty())
        val checks = JSONArray(it.checklist)
        require(checks.length() <= 10000)
        (0 until checks.length()).forEach { index ->
            val check = checks.getJSONObject(index)
            require(check.getString("text").isNotBlank())
            check.getBoolean("done")
        }
    }
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val root = JSONObject().put("format", "LifeMate").put("version", 2).put("exportedAt", Instant.now().toString())
        val media = mutableSetOf<String>()
        repo.db.withTransaction {
            val profile = requireNotNull(repo.dao.getProfile())
            root.put("profile", profile.json())
            if (profile.photo.isNotBlank()) media.add(profile.photo)
            val posts = repo.dao.allPosts()
            posts.filter { it.photo.isNotBlank() }.forEach { media.add(it.photo) }
            root.put("posts",JSONArray(posts.map { JSONObject().put("id",it.id).put("type",it.type).put("topic",it.topic).put("message",it.message).put("person",it.person).put("date",it.date).put("caption",it.caption).put("photo",File(it.photo).name).put("createdAt",it.createdAt).put("updatedAt",it.updatedAt) }))
            root.put("items", JSONArray(repo.dao.allItems().map { it.json() }))
            root.put("completions", JSONArray(repo.dao.allCompletions().map { JSONObject().put("itemId", it.itemId).put("date", it.date).put("completedAt", it.completedAt) }))
            root.put("attachments", JSONArray(repo.dao.allAttachments().map {
                media.add(it.path)
                JSONObject().put("id", it.id).put("itemId", it.itemId).put("file", File(it.path).name).put("mime", it.mime).put("name", it.name)
            }))
        }
        ZipOutputStream(requireNotNull(repo.context.contentResolver.openOutputStream(uri))).use { zip ->
            zip.putNextEntry(ZipEntry("lifemate.json")); zip.write(root.toString(2).toByteArray()); zip.closeEntry()
            media.forEach { path ->
                val file = requireNotNull(repo.safeMedia(path)) { "Unsafe attachment path." }
                require(file.exists()) { "A media file is missing; backup was not completed." }
                zip.putNextEntry(ZipEntry("media/${file.name}")); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }
    suspend fun restore(uri: Uri) = withContext(Dispatchers.IO) {
        val staging = File(repo.context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        val copied = mutableListOf<File>()
        var committed = false
        try {
            var size = 0L; var entries = 0
            ZipInputStream(requireNotNull(repo.context.contentResolver.openInputStream(uri))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(++entries <= 100_000 && !entry.isDirectory) { "Unsupported backup archive." }
                    val path = entry.name
                    require(path == "lifemate.json" || Regex("media/[A-Za-z0-9._-]+").matches(path)) { "Unsafe archive path." }
                    val file = File(staging, path).canonicalFile
                    require(file.path.startsWith(staging.canonicalPath + File.separator) && !file.exists()) { "Invalid or duplicate archive entry." }
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out ->
                        val buffer = ByteArray(65536)
                        while (true) { val n = zip.read(buffer); if (n < 0) break; size += n; require(size <= 2L * 1024 * 1024 * 1024) { "Backups larger than 2 GB are not supported." }; out.write(buffer, 0, n) }
                    }
                }
            }
            val manifest = File(staging, "lifemate.json")
            require(manifest.length() <= 32L * 1024 * 1024) { "Backup manifest is too large." }
            val root = JSONObject(manifest.readText())
            require(root.getString("format") == "LifeMate" && root.getInt("version") in 1..2) { "Unsupported backup version." }
            val pj = root.getJSONObject("profile")
            val remapped = mutableMapOf<String, String>()
            fun media(name: String): String {
                if (name.isBlank()) return ""
                return remapped.getOrPut(name) {
                    require(Regex("[A-Za-z0-9._-]+").matches(name))
                    val source = File(staging, "media/$name")
                    require(source.isFile) { "Backup is missing a media file." }
                    val target = File(repo.mediaDir, UUID.randomUUID().toString())
                    copied.add(target); source.copyTo(target); target.absolutePath
                }
            }
            val profile = Profile(fullName = pj.getString("fullName"), nickname = pj.optString("nickname"), preferredName = pj.optString("preferredName"),
                birthday = pj.optString("birthday"), photo = media(pj.optString("photo")), introduction = pj.optString("introduction"), information = pj.optString("information"))
            require(profile.fullName.isNotBlank())
            if (profile.birthday.isNotBlank()) LocalDate.parse(profile.birthday)
            val items = root.getJSONArray("items").objects().map(::parseItem)
            val ids = items.map { it.id }.toSet(); require(ids.size == items.size)
            val completions = root.getJSONArray("completions").objects().map {
                require(it.getString("itemId") in ids)
                Completion(it.getString("itemId"), LocalDate.parse(it.getString("date")).toString(), it.getLong("completedAt"))
            }
            val attachments = root.getJSONArray("attachments").objects().map {
                require(it.getString("itemId") in ids)
                val mime = it.getString("mime"); require(mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/"))
                Attachment(UUID.fromString(it.getString("id")).toString(), it.getString("itemId"), media(it.getString("file")), mime, it.getString("name"))
            }
            val posts = (if(root.getInt("version")==2) root.getJSONArray("posts") else JSONArray()).objects().map { j ->
                SocialPost(id=UUID.fromString(j.getString("id")).toString(),type=j.getString("type"),topic=j.optString("topic"),message=j.optString("message"),person=j.optString("person"),date=j.optString("date"),caption=j.optString("caption"),photo=media(j.optString("photo")),createdAt=j.getLong("createdAt"),updatedAt=j.getLong("updatedAt")).also { PostTemplates.validate(it) }
            }
            require(posts.map { it.id }.toSet().size==posts.size) { "Duplicate post IDs." }
            val oldFiles = repo.mediaDir.listFiles()?.filter { it !in copied }.orEmpty()
            val oldItems = repo.dao.allItems()
            repo.db.withTransaction {
                repo.dao.clear(); repo.dao.saveProfile(profile)
                posts.forEach { repo.dao.savePost(it) }; items.forEach { repo.dao.save(it) }; completions.forEach { repo.dao.complete(it) }; attachments.forEach { repo.dao.saveAttachment(it) }
            }
            committed = true
            val app = repo.context.applicationContext as com.lifemate.LifeMateApp
            oldItems.forEach { app.scheduler.cancel(it.id) }
            oldFiles.forEach { it.delete() }
            ReminderScheduler.enqueueReschedule(repo.context)
        } finally {
            if (!committed) copied.forEach { it.delete() }
            staging.deleteRecursively()
        }
    }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
}
