package com.lifemate.ui

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lifemate.*
import com.lifemate.domain.*
import java.time.*

@Composable fun NotificationSettings(state: LifeState, vm: LifeViewModel) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (!it) vm.message("Notifications are off. LifeMate still works; reminders will remain in the app."); refresh++ }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++; vm.runAction { vm.app.scheduler.reconcile() } }
    // Reading refresh ensures permission controls update after returning from Android Settings.
    @Suppress("UNUSED_VARIABLE") val permissionRevision = refresh
    val allowed = vm.app.scheduler.permissionGranted()
    ToggleRow("Notifications", "Allow LifeMate to schedule your personal reminders.", state.preferences.notifications) { vm.preference("notifications", it) }
    if (!allowed) Button({ if (Build.VERSION.SDK_INT >= 33) request.launch(Manifest.permission.POST_NOTIFICATIONS) else settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Allow notification permission") }
    OutlinedButton({ settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }, Modifier.fillMaxWidth()) { Text("Android notification settings") }
    if (Build.VERSION.SDK_INT >= 31) {
        Text(if (vm.app.scheduler.canBePrecise()) "Precise alarms are allowed." else "Reminders use approximate timing. Android may delay them while idle.", style = MaterialTheme.typography.bodyMedium)
        if (!vm.app.scheduler.canBePrecise()) OutlinedButton({ try { settings.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) } catch (_: ActivityNotFoundException) { vm.message("Precise alarm settings aren't available on this device.") } }, Modifier.fillMaxWidth()) { Text("Allow precise reminders") }
    }
    ToggleRow("Voice reminders", "Read a short reminder aloud with an installed offline English/Bangla voice. Off by default; people nearby can hear it. Silent mode, DND, locked screens and app PIN lock stay silent.", state.preferences.voiceReminders) { vm.preference("voiceReminders", it) }
    if (state.preferences.voiceReminders) {
        OutlinedButton({ com.lifemate.notifications.VoiceReminderService.speak(context,"LifeMate reminder. It is time for your next small step."); vm.message("Voice needs an installed offline English voice, notification permission and an unlocked, non-silent phone. If unavailable, reminders remain as text.") }, Modifier.fillMaxWidth()) { Text("Test voice reminder") }
        TextButton({ try { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) } catch (_: Exception) { vm.message("Open Android Settings and search for Text-to-speech to install an offline voice.") } }) { Text("Android voice settings") }
        Text("Speech lasts at most 30 seconds, with a visible Stop notification. Android may block background speech for approximate alarms; the normal notification remains.",style = MaterialTheme.typography.bodyMedium)
    }
    Text("Default sound", style = MaterialTheme.typography.titleMedium)
    SoundPicker(state.preferences.sound) { vm.preference("sound", it) }
    ToggleRow("Vibration", "Global preference; individual reminders may be silent.", state.preferences.vibration) { vm.preference("vibration", it) }
    Text("Android controls notification channels, Do Not Disturb, and battery restrictions. A force-stopped app cannot deliver alarms until reopened. Pending reminders can recover within 12 hours; older occurrences are skipped.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton({ try { settings.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: ActivityNotFoundException) { vm.message("Battery settings are unavailable.") } }) { Text("Review Android battery settings") }
}
@Composable fun SettingsScreen(activity: MainActivity, state: LifeState, vm: LifeViewModel, navigate: (String) -> Unit, onLockChanged: () -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    var confirmation by remember { mutableStateOf<String?>(null) }
    var pinDialog by remember { mutableStateOf<String?>(null) }
    var locked by remember { mutableStateOf(vm.app.secure.hasPin()) }
    var deletingText by remember { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) vm.export(uri) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.restore(uri) { navigate("home") } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeading("Make it feel like you", "Your preferences. Your privacy. Your LifeMate.")
        SoftCard(Modifier.fillMaxWidth()) {
            SectionHeading("Your profile")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Avatar(state.profile); Text(state.profile?.fullName ?: "Your profile", style = MaterialTheme.typography.titleMedium) }
            OutlinedButton({ navigate("edit-profile") }, Modifier.fillMaxWidth()) { Text("Edit personal information") }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            SectionHeading("Keep LifeMate current")
            Text("Installed version ${BuildConfig.VERSION_NAME} · verified signed releases only")
            OutlinedButton({ navigate("updates") }, Modifier.fillMaxWidth()) { Text("App updates") }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            SectionHeading("Feedback")
            ToggleRow("Sound effects", "Subtle completion and success tones. Silent mode and DND are respected.",state.preferences.soundEffects) {vm.preference("soundEffects",it)}
        }
        SoftCard(Modifier.fillMaxWidth()) { PostAiSettings(state,vm) }
        SoftCard { SectionHeading("Notifications"); NotificationSettings(state, vm) }
        SoftCard(Modifier.fillMaxWidth().testTag("appearance-settings")) { SectionHeading("Appearance"); ChoiceChips(listOf("Light", "Dark", "System"), state.preferences.theme) { vm.preference("theme", it) } }
        SoftCard(Modifier.fillMaxWidth()) {
            SectionHeading("Your private space")
            ToggleRow("App lock", "PIN protection; locks after 30 seconds away. Screenshots are blocked while enabled.", locked) { pinDialog = if (it) "set" else "remove" }
            if (locked) {
                TextButton({ pinDialog = "set" }) { Text("Change PIN") }
                ToggleRow("Biometric unlock", if (biometricAvailable(activity)) "Use a strong fingerprint or face biometric." else "Set up a supported strong biometric in Android settings.", state.preferences.biometric) {
                    if (!it || biometricAvailable(activity)) vm.preference("biometric", it) else vm.message("No supported biometric is enrolled.")
                }
            }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            SectionHeading("Your data belongs to you")
            Text("Back up your profile, records, completions, and media to a portable ZIP. PINs and device settings aren't exported.", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton({ confirmation = "export" }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.FileUpload, null); Text(" Export / back up data") }
            OutlinedButton({ confirmation = "restore" }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.FileDownload, null); Text(" Import / restore backup") }
            TextButton({ confirmation = "delete"; deletingText = "" }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error); Text(" Delete all data", color = MaterialTheme.colorScheme.error) }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            SectionHeading("About LifeMate")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandMark(44.dp)
                Text("Your Personal Life Assistant.\nVersion ${BuildConfig.VERSION_NAME}")
            }
            TextButton({ navigate("privacy") }) { Text("Privacy policy & data security") }
            TextButton({ navigate("terms") }) { Text("Terms & reliability notes") }
            TextButton({ try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thisrony506-prog/LifeMate/issues"))) } catch (_: Exception) { vm.message("Open github.com/thisrony506-prog/LifeMate/issues in a browser for support.") } }) { Text("Contact / support on GitHub") }
        }
        Spacer(Modifier.height(24.dp))
    }
    pinDialog?.let { mode -> PinSetupDialog(vm.app.secure, mode == "remove", { pinDialog = null }) { locked = vm.app.secure.hasPin(); if (!locked) vm.preference("biometric", false); pinDialog = null; onLockChanged() } }
    when (confirmation) {
        "export" -> ConfirmDialog("Export a private backup?", "The ZIP includes your personal information and media and is NOT encrypted. Anyone with the file can read it. Choose a private location. LifeMate never uploads it automatically.", "Choose location", { confirmation = null }) { confirmation = null; export.launch("LifeMate-backup-${LocalDate.now()}.zip") }
        "restore" -> ConfirmDialog("Replace data from a backup?", "Restoring replaces ALL current profile data, records, and media. Export a backup first. Only LifeMate ZIP backups are supported (up to 2 GB). Your current PIN is kept.", "Choose backup", { confirmation = null }) { confirmation = null; restore.launch(arrayOf("application/zip", "application/octet-stream")) }
        "delete" -> AlertDialog(onDismissRequest = { confirmation = null }, title = { Text("Permanently delete everything?") }, text = { Column { Text("All LifeMate records, recordings, photos, and progress will be removed from this device. Backups you exported aren't deleted. Type DELETE to confirm."); Field(deletingText, { deletingText = it }, "Confirmation") } }, confirmButton = { TextButton({ confirmation = null; vm.runAction("All local data deleted", { onLockChanged(); onReset() }) { vm.repo.deleteAll(); vm.app.secure.removePin() } }, enabled = deletingText == "DELETE") { Text("Delete everything", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ confirmation = null }) { Text("Cancel") } })
    }
}
@Composable fun NotificationsScreen(state: LifeState, vm: LifeViewModel, navigate: (String) -> Unit) {
    val upcoming = state.items.filter { !it.archived && it.notifications }.mapNotNull { item -> Schedule.next(item.spec(), Instant.now(), ZoneId.systemDefault())?.let { item to it } }.sortedBy { it.second }
    androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeading("Gentle nudges", "What's coming up in your LifeMate.") }
        item { SoftCard {
            Text(if (vm.app.scheduler.permissionGranted() && state.preferences.notifications) "Notifications are ready" else "Notifications are currently off", style = MaterialTheme.typography.titleMedium)
            Text("${if (vm.app.scheduler.canBePrecise()) "Precise" else "Approximate"} timing · no intrusive full-screen alerts", style = MaterialTheme.typography.bodyMedium)
            TextButton({ navigate("settings") }) { Text("Manage reminder settings") }
        } }
        if (upcoming.isEmpty()) item { EmptyState(Kind.REMINDER, "Nothing waiting in the wings", "Add a routine, reminder, birthday, or mission to see its next nudge here.", "New reminder") { navigate("edit/REMINDER/new") } }
        upcoming.forEach { (record, time) -> item(key = record.id) { Text(time.atZone(ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")), style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(8.dp)); ItemRow(record, state, { navigate("detail/${record.id}") }) } }
    }
}
@Composable fun PolicyScreen(privacy: Boolean) {
    val sections = if (privacy) listOf(
        "Local by design" to "Life Mate has no analytics or advertising. Core tools work offline without an account. The new Flutter vault uses encrypted Hive records and AES-GCM photos. An optional Firebase account supports explicitly requested encrypted record sync; a recovery phrase stays on your device. Optional Sathi sends only the message you approve to a Gemini backend, not journal or health records. Family sharing is opt-in, encrypted, foreground-only and expires after 15 minutes. The retained Android organizer remains in its original encrypted database. The internet permission is used for public update checks, requested APK downloads and optional post generation through a user-configured HTTPS backend. AI requests require explicit confirmation and send only the displayed post fields, never photos or other records. Provider API keys belong on the backend, not in LifeMate. Update controls are in Settings. A verified newer signed version requires installation before continuing; no connection or server error alone does not lock the app. Manual checks and downloads require a connection. APK updates download inside LifeMate, with signed-digest and installed-key checks before Android installation confirmation. Installation-source permission is requested only when you tap Install update. GitHub receives normal connection information and the app version, never your personal records. Core features work offline. Post generation uses offline templates unless you explicitly enable and approve an AI request. The selected name, date and writing fields may contain personal information; only send them to a backend you trust. Birthday wishes and cards work offline.",
        "Protected storage" to "Profile data, notes, schedules, and metadata are stored in a SQLCipher-encrypted Room database. Its random key is encrypted using Android Keystore. Media and audio are stored in the app's private internal directory, protected by Android's sandbox and device encryption; they are not separately encrypted by LifeMate. Use a device screen lock for stronger protection.",
        "App lock" to "Optional PINs are salted and hashed with PBKDF2-HMAC-SHA256, then encrypted with a Keystore key. Five failed attempts cause a one-minute cooldown. Strong Android biometrics can unlock the app when enabled. The app locks after 30 seconds in the background; notification details may remain visible according to Android lock-screen settings.",
        "Permissions" to "Notification permission is requested when you enable reminders. Precise alarms are optional. Microphone access is requested only when recording. Photos and videos are selected through Android's system picker; no broad gallery permission is used. LifeMate never records in a background service. Optional voice reminders use a short, visible playback service and an installed offline TTS voice; they respect silent mode, DND, screen lock and app lock. Studio photos are copied privately and exported without source location metadata.",
        "Sharing & backups" to "Sharing only happens when you tap Share and choose another app. Retained-organizer ZIP backups are unencrypted and include its selected media and personal information, not the new Flutter vault. Export the new vault separately from Flutter Profile using a passphrase-encrypted backup. Clearing local app data does not delete cloud copies. Android's system document picker may offer cloud providers—choose one only if you want that provider to receive the file. Automatic Android cloud backup is disabled. Import validates files before replacing records.",
        "Your control" to "Edit, export, or delete your data at any time. Deleting an attachment removes LifeMate's private copy, not the original gallery file. Deleting all data does not delete backups or images you saved outside the app. Uninstalling deletes local storage. We cannot recover lost PINs or unexported data.",
        "Support" to "Support is available through the LifeMate GitHub issue tracker. Do not post private notes, birthdays, backups, or medical information to public issues. Opening support is an explicit browser action, not a background upload."
    ) else listOf(
        "A personal organizer" to "LifeMate is a general-purpose organizer, not a medical, emergency, or safety-critical alarm system. Generic medicine reminders make no medical claims. Do not rely on it as your sole reminder for essential treatment or urgent obligations.",
        "Android reminder limits" to "Reminders run through AlarmManager, notification channels, and a periodic WorkManager reconciliation task. Android may delay inexact alarms, rate-limit precise alarms during idle, silence channels, or restrict background execution. A force-stopped app cannot run until reopened. No notifications can be delivered while the device is powered off. Persisted pending reminders can recover up to 12 hours late after a restart. Older occurrences are skipped and the next future occurrence is scheduled.",
        "Time & recurrence" to "Times are local wall-clock times and follow timezone changes. A daylight-saving gap moves to the next valid local time; an overlap uses the first offset. Monthly schedules clamp to the final day in shorter months. February 29 birthdays are observed February 28 in non-leap years. Completing a task skips its reminder for that date.",
        "Progress" to "Mission progress counts completed scheduled days. Remaining calendar days and unfinished daily targets are different measures. Habit streaks count consecutive calendar days. Historical charts use current record schedules; editing or deleting a record can change aggregated statistics.",
        "Storage limits" to "There is no artificial record-count limit in ordinary use; available device storage is the practical limit. Individual media imports are limited to 250 MB, voice recordings to 10 minutes, and backup imports to 2 GB / 100,000 files / 32 MB of JSON metadata. Export before changing or uninstalling the app.",
        "Use & availability" to "This initial release is provided as-is, without guarantees of uninterrupted operation. Review generated greetings before sharing. You are responsible for rights to media you attach and for storing exported backups securely."
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageHeading(if (privacy) "Private, by nature." else "A few things to know", if (privacy) "Privacy policy · 13 Sep 2026" else "Terms & reliability · LifeMate")
        sections.forEach { (title, text) -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}

@Composable private fun PostAiSettings(state: LifeState, vm: LifeViewModel) {
    var editing by remember { mutableStateOf(false) }
    var endpoint by androidx.compose.runtime.saveable.rememberSaveable(state.preferences.postAiEndpoint) { mutableStateOf(state.preferences.postAiEndpoint) }
    SectionHeading("Optional post AI")
    Text(if(state.preferences.postAiEndpoint.isBlank()) "Not configured · offline templates available" else "Custom backend · asks before every request",style=MaterialTheme.typography.bodyMedium)
    TextButton({editing=!editing}) {Text(if(editing) "Hide setup" else "Configure backend")}
    if(editing) {
        Field(endpoint,{endpoint=it.take(500)},"HTTPS backend endpoint")
        Text("Use a trusted server that keeps its AI API key server-side. Do not paste provider keys or tokens here. No automatic uploads; photos are never sent.",style=MaterialTheme.typography.bodyMedium)
        Button({
            if(endpoint.isBlank() || com.lifemate.domain.PostAiConfig.validEndpoint(endpoint.trim())) {vm.preference("postAiEndpoint",endpoint.trim());editing=false}
            else vm.message("Use an HTTPS endpoint without credentials, query parameters or fragments.")
        }) {Text("Save configuration")}
        if(state.preferences.postAiEndpoint.isNotBlank()) TextButton({vm.preference("postAiEndpoint","");endpoint="";editing=false}) {Text("Disable AI")}
    }
}
