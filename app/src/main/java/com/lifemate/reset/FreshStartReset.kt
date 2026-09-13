package com.lifemate.reset

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.job.JobScheduler
import android.content.Context
import android.os.Build
import java.io.File
import java.nio.file.Files
import java.security.KeyStore

/** Explicitly requested clean replacement. Runs BEFORE a Flutter vault is opened.
 * Never reset again on future launches/upgrades. Preserve only authenticated
 * updater metadata, not personal records. Exports/cloud copies are outside scope.
 */
object FreshStartReset {
    const val MARKER = "personal-os-fresh-start-v1"
    fun run(context: Context): Boolean {
        val marker = File(context.noBackupFilesDir, MARKER)
        if (marker.isFile) return false
        context.getSystemService(JobScheduler::class.java).cancelAll()
        context.getSystemService(NotificationManager::class.java).cancelAll()
        if (Build.VERSION.SDK_INT >= 34) context.getSystemService(AlarmManager::class.java).cancelAll()
        // Pre-34 alarm intents target removed receivers and cannot deliver. New
        // reminders use the Flutter notification plugin's different receiver.
        context.databaseList().forEach { name ->
            check(context.deleteDatabase(name) || !context.getDatabasePath(name).exists())
        }
        val prefs = File(context.applicationInfo.dataDir, "shared_prefs")
        prefs.listFiles()?.filter { it.extension == "xml" && it.name != "release_updates.xml" }?.forEach {
            check(context.deleteSharedPreferences(it.nameWithoutExtension))
        }
        clearChildren(prefs, setOf("release_updates.xml", "release_updates.xml.bak"))
        clearChildren(context.filesDir)
        clearChildren(context.cacheDir)
        clearChildren(context.noBackupFilesDir)
        context.getExternalFilesDirs(null).filterNotNull().forEach { clearChildren(it) }
        context.externalCacheDirs.filterNotNull().forEach { clearChildren(it) }
        // Android signing identity is NOT here: these are this app's local data keys.
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keys.aliases().toList().forEach { keys.deleteEntry(it) }
        marker.parentFile?.mkdirs()
        marker.outputStream().use { it.write("fresh-start-v1\n".toByteArray()); it.fd.sync() }
        return true
    }

    internal fun clearChildren(directory: File, keep: Set<String> = emptySet()) {
        directory.listFiles()?.filter { it.name !in keep }?.forEach { erase(it) }
    }
    private fun erase(file: File) {
        // Do not follow a symlink out to a user's gallery or other shared storage.
        if (!Files.isSymbolicLink(file.toPath()) && file.isDirectory) clearChildren(file)
        check(file.delete() || !file.exists()) { "Fresh start could not clear private storage" }
    }
}
