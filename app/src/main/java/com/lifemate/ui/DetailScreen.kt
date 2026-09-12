package com.lifemate.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.lifemate.database.*
import com.lifemate.domain.*
import com.lifemate.utils.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.io.File
import java.time.*
import java.time.temporal.ChronoUnit

@Composable fun DetailScreen(item: LifeItem, state: LifeState, vm: LifeViewModel, navigate: (String) -> Unit, onDeleted: () -> Unit) {
    FeatureTheme(item.kind) { FeatureDetailContent(item, state, vm, navigate, onDeleted) }
}
@Composable private fun FeatureDetailContent(item: LifeItem, state: LifeState, vm: LifeViewModel, navigate: (String) -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var delete by remember { mutableStateOf(false) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.attach(item, uri) }
    var removeAttachment by remember { mutableStateOf<Attachment?>(null) }
    var renameAttachment by remember { mutableStateOf<Attachment?>(null) }
    var rename by remember { mutableStateOf("") }
    var video by remember { mutableStateOf<Attachment?>(null) }
    var goalProgress by remember(item.progress) { mutableFloatStateOf(item.progress.toFloat()) }
    LazyColumn(contentPadding = PaddingValues(22.dp, 16.dp, 22.dp, 36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val portrait = if (item.kind == Kind.BIRTHDAY) state.media(item).firstOrNull { it.mime.startsWith("image/") } else null
            if (portrait != null) AsyncImage(File(portrait.path), "${item.title}'s photo", Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            else KindBadge(item.kind, 56.dp)
            Spacer(Modifier.weight(1f))
            IconButton({ vm.save(item.copy(pinned = !item.pinned)) {} }) { Icon(Icons.Outlined.PushPin, if (item.pinned) "Unpin" else "Pin", tint = if (item.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton({ navigate("edit/${item.kind.name}/${item.id}") }) { Icon(Icons.Outlined.Edit, "Edit ${item.kind.label}") }
            IconButton({ delete = true }) { Icon(Icons.Outlined.DeleteOutline, "Delete ${item.kind.label}") }
        } }
        item { Eyebrow("${item.kind.label}${if (item.archived) " · archived" else ""}"); Spacer(Modifier.height(8.dp)); Text(item.title, style = MaterialTheme.typography.headlineLarge); if (item.description.isNotBlank()) { Spacer(Modifier.height(12.dp)); androidx.compose.foundation.text.selection.SelectionContainer { Text(item.description, style = MaterialTheme.typography.bodyLarge) } } }
        item { SoftCard(Modifier.fillMaxWidth()) {
            Text("${LocalDate.parse(item.date).format(dateFormat)}${if (item.kind !in setOf(Kind.NOTE, Kind.MEMORY, Kind.BIRTHDAY)) " · ${LocalTime.parse(item.time).format(timeFormat)}" else ""}", style = MaterialTheme.typography.titleMedium)
            if (item.kind in setOf(Kind.ROUTINE, Kind.HABIT, Kind.REMINDER)) Text(item.repeat.label, style = MaterialTheme.typography.bodyMedium)
            if (item.dailyTarget.isNotBlank()) Text("Daily target: ${item.dailyTarget}")
            if (item.tags.isNotBlank()) Text(item.tags.split(',').joinToString("   ") { "#${it.trim()}" }, color = MaterialTheme.colorScheme.primary)
            if (item.kind in taskKinds && item.occurs(today)) Button({ vm.toggle(item) }, Modifier.fillMaxWidth()) { Icon(if (state.done(item)) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null); Text(if (state.done(item)) "  Completed today · undo" else "  Mark today complete") }
            else if (item.kind in taskKinds) Text(if (LocalDate.parse(item.date) > today) "Starts ${LocalDate.parse(item.date).format(dateFormat)}" else "Not scheduled for today", style = MaterialTheme.typography.bodyMedium)
            if (item.notifications && item.kind !in setOf(Kind.NOTE, Kind.MEMORY)) Text(when {
                !state.preferences.notifications -> "Reminders are turned off in Settings"
                !vm.app.scheduler.permissionGranted() -> "Reminders need notification permission in Settings"
                item.archived || item.kind == Kind.GOAL && item.progress == 100 -> "No reminder scheduled for this completed or archived record"
                Schedule.next(item.spec(), Instant.now(), ZoneId.systemDefault()) == null -> "No future reminder scheduled"
                else -> "Reminders enabled · ${if (vm.app.scheduler.canBePrecise()) "precise" else "approximate"} timing"
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        if (item.kind == Kind.MISSION) item {
            val completed = state.dates(item).count { Schedule.occurs(item.spec(), it) }
            val current = (ChronoUnit.DAYS.between(LocalDate.parse(item.date), today) + 1).coerceIn(0, item.duration.toLong())
            val remaining = (item.duration - current).coerceAtLeast(0)
            SoftCard(Modifier.fillMaxWidth(), MaterialTheme.colorScheme.primaryContainer) {
                Eyebrow("YOUR JOURNEY")
                Text("Day $current / ${item.duration}", style = MaterialTheme.typography.headlineMedium)
                LinearProgressIndicator(progress = { state.progress(item) }, Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                Text("$completed completed days · ${(state.progress(item) * 100).toInt()}%")
                Text("$remaining calendar days remaining · ${item.duration - completed} targets unfinished", style = MaterialTheme.typography.bodyMedium)
                Text("${Schedule.currentStreak(state.dates(item), today)} day streak · ${Schedule.bestStreak(state.dates(item))} day best", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (item.kind in setOf(Kind.MISSION, Kind.HABIT, Kind.ROUTINE)) item { CompletionCalendar(item, state, vm) }
        if (item.kind == Kind.GOAL) item { SoftCard {
            SectionHeading("A little closer")
            Text("${goalProgress.toInt()}% complete", style = MaterialTheme.typography.headlineMedium)
            Slider(goalProgress, { goalProgress = it }, valueRange = 0f..100f, steps = 99)
            Button({ vm.save(item.copy(progress = goalProgress.toInt())) {} }, enabled = goalProgress.toInt() != item.progress) { Text("Update progress") }
        } }
        if (item.checklist != "[]") item { SoftCard {
            SectionHeading(if (item.kind == Kind.GOAL) "Milestones" else "Your checklist")
            val array = JSONArray(item.checklist)
            (0 until array.length()).forEach { index -> val entry = array.getJSONObject(index)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(entry.optBoolean("done"), { checked -> val changed = JSONArray(item.checklist); changed.getJSONObject(index).put("done", checked); vm.save(item.copy(checklist = changed.toString())) {} })
                    Text(entry.getString("text"), Modifier.weight(1f))
                }
            }
        } }
        if (item.kind == Kind.BIRTHDAY) item { SoftCard(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .5f)) {
            Text("${item.relationship}${if (item.nickname.isNotBlank()) " · ${item.nickname}" else ""}", style = MaterialTheme.typography.titleMedium)
            val next = Schedule.nextBirthday(LocalDate.parse(item.date), today)
            Text(if (next == today) "It's their special day!" else "${ChronoUnit.DAYS.between(today, next)} days until their birthday", style = MaterialTheme.typography.headlineMedium)
            Text("Remind ${item.birthdayOffsets.split(',').filter { it.isNotBlank() }.joinToString { if (it == "0") "on the day" else "$it days before" }}", style = MaterialTheme.typography.bodyMedium)
            Button({ navigate("wish/${item.id}") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AutoAwesome, null); Text("  Create birthday wish") }
        } }
        if (item.notes.isNotBlank()) item { SoftCard(Modifier.fillMaxWidth()) { Eyebrow("PERSONAL NOTES"); androidx.compose.foundation.text.selection.SelectionContainer { Text(item.notes) } } }
        item { SectionHeading(if (item.kind == Kind.MEMORY) "The moments" else "Photos, videos & voice", "Add media") { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) } }
        items(state.media(item), key = { it.id }) { attachment ->
            SoftCard(Modifier.fillMaxWidth()) {
                if (attachment.mime.startsWith("image/")) AsyncImage(File(attachment.path), attachment.name, Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(14.dp)).clickable { try { shareFile(context, File(attachment.path), attachment.mime, true) } catch (_: Exception) { vm.message("No compatible app can open this file.") } }, contentScale = ContentScale.Fit)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(attachment.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton({ renameAttachment = attachment; rename = attachment.name }) { Icon(Icons.Outlined.Edit, "Rename attachment") }
                    IconButton({ removeAttachment = attachment }) { Icon(Icons.Outlined.DeleteOutline, "Delete attachment") }
                }
                if (attachment.mime.startsWith("audio/")) AudioPlayer(attachment.path, vm)
                Row {
                    if (attachment.mime.startsWith("video/")) TextButton({ video = attachment }) { Icon(Icons.Outlined.PlayCircle, null); Text(" Play video") }
                    TextButton({ try { shareFile(context, File(attachment.path), attachment.mime) } catch (_: Exception) { vm.message("Unable to share this file.") } }) { Icon(Icons.Outlined.Share, null); Text(" Share") }
                }
            }
        }
        item { VoiceNotePanel(item, vm) }
        item { OutlinedButton({ vm.save(item.copy(archived = !item.archived)) {} }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Archive, null); Text(if (item.archived) "  Restore from archive" else "  Archive ${item.kind.label.lowercase()}") } }
    }
    video?.let { VideoDialog(it, vm) { video = null } }
    if (delete) ConfirmDialog("Delete ${item.kind.label.lowercase()}?", "“${item.title}”, its progress, and its attachments will be permanently deleted.", onDismiss = { delete = false }) { delete = false; vm.delete(item, onDeleted) }
    removeAttachment?.let { attachment -> ConfirmDialog("Delete attachment?", "This file will be removed from LifeMate. The original in your gallery isn't affected.", onDismiss = { removeAttachment = null }) { removeAttachment = null; vm.runAction("Attachment deleted") { vm.repo.deleteAttachment(attachment) } } }
    renameAttachment?.let { attachment -> AlertDialog(onDismissRequest = { renameAttachment = null }, title = { Text("Rename attachment") }, text = { Field(rename, { rename = it }, "Name") }, confirmButton = { TextButton({ if (rename.isNotBlank()) { vm.runAction { vm.repo.dao.saveAttachment(attachment.copy(name = rename.trim())) }; renameAttachment = null } }) { Text("Save") } }, dismissButton = { TextButton({ renameAttachment = null }) { Text("Cancel") } }) }
}
@Composable fun CompletionCalendar(item: LifeItem, state: LifeState, vm: LifeViewModel) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText); val today = LocalDate.now()
    val dates = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val due = dates.count { item.occurs(it) && it <= today }; val completed = dates.count { item.occurs(it) && it <= today && state.done(item, it) }
    val weekDays = (0L..6L).map { today.minusDays(it) }.filter { item.occurs(it) }
    SoftCard {
        SectionHeading("Showed up for yourself")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton({ monthText = month.minusMonths(1).toString() }) { Text("Previous") }; Text(month.toString(), style = MaterialTheme.typography.titleMedium); TextButton({ monthText = month.plusMonths(1).toString() }) { Text("Next") }
        }
        dates.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            week.forEach { day -> val done = state.done(item, day); val active = item.occurs(day) && day <= today
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).background(if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (active) 1f else .3f))
                    .clickable(enabled = active) { vm.toggle(item, day) }.semantics { contentDescription = "$day: ${if (done) "completed" else if (active) "not completed" else "not available"}" }, contentAlignment = Alignment.Center) { Text(day.dayOfMonth.toString(), color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)) }
            }
            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
        } }
        Text("$completed / $due this month · ${if (due == 0) 0 else completed * 100 / due}%", style = MaterialTheme.typography.bodyMedium)
        Text("Last 7 days: ${weekDays.count { state.done(item, it) }} / ${weekDays.size} · Streak: ${Schedule.currentStreak(state.dates(item), today)} · Best: ${Schedule.bestStreak(state.dates(item))}", style = MaterialTheme.typography.bodyMedium)
        Text("Tap a past or current scheduled day to update it. Streaks count consecutive calendar days.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun AudioPlayer(path: String, vm: LifeViewModel) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(path, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { runCatching { player?.pause() }; playing = false } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); player?.release(); player = null }
    }
    TextButton({
        try {
            if (playing) { runCatching { player?.pause() }; playing = false }
            else if (player != null) { player?.start(); playing = true }
            else {
                val newPlayer = MediaPlayer(); player = newPlayer
                newPlayer.setDataSource(path)
                newPlayer.setOnPreparedListener { it.start(); playing = true }
                newPlayer.setOnCompletionListener { playing = false; it.seekTo(0) }
                newPlayer.setOnErrorListener { _, _, _ -> playing = false; player?.release(); player = null; vm.message("Unable to play this recording."); true }
                newPlayer.prepareAsync()
            }
        } catch (_: Exception) { player?.release(); player = null; playing = false; vm.message("Unable to play this recording.") }
    }) { Icon(if (playing) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle, null); Text(if (playing) " Pause recording" else " Play recording") }
}
@Composable fun VoiceNotePanel(item: LifeItem, vm: LifeViewModel) {
    val context = LocalContext.current; val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }; var paused by remember { mutableStateOf(false) }; var seconds by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current
    fun finish() {
        val file = recorder.stop(); recording = false; paused = false
        if (file != null) vm.runAction("Voice note saved") { try { vm.repo.dao.saveAttachment(Attachment(itemId = item.id, path = file.absolutePath, mime = "audio/mp4", name = "Voice note · ${LocalDate.now()}")) } catch (e: Exception) { file.delete(); throw e } }
        else vm.message("Recording was too short to save. Please try again.")
    }
    fun begin() { try { recorder.start(); recording = true; paused = false; seconds = 0 } catch (_: Exception) { vm.message("Microphone unavailable. Check permission or close another recording app.") } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) begin() else vm.message("Microphone permission denied. You can still use text notes and media attachments.") }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP && recording) finish() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); if (recording) finish() else recorder.stop(true) }
    }
    LaunchedEffect(recording, paused) { while (recording && !paused) { delay(1000); seconds++; if (seconds >= 590) finish() } }
    SoftCard(Modifier.fillMaxWidth()) {
        Text("A thought, in your own voice", style = MaterialTheme.typography.titleMedium)
        Text(if (recording) "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} · ${if (paused) "Paused" else "Recording on this device"}" else "Private, local recordings · up to 10 minutes", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!recording) OutlinedButton({ if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) begin() else permission.launch(Manifest.permission.RECORD_AUDIO) }) { Icon(Icons.Outlined.Mic, null); Text(" Record voice note") }
            else {
                OutlinedButton({ try { if (paused) recorder.resume() else recorder.pause(); paused = !paused } catch (_: Exception) { finish() } }) { Text(if (paused) "Resume" else "Pause") }
                Button({ finish() }) { Icon(Icons.Outlined.Stop, null); Text(" Save") }
                IconButton({ recorder.stop(true); recording = false; paused = false }) { Icon(Icons.Outlined.DeleteOutline, "Discard recording") }
            }
        }
    }
}

@Composable fun VideoDialog(attachment: Attachment, vm: LifeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val video = remember(attachment.path) { android.widget.VideoView(context) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(video, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) runCatching { video.pause() } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); video.stopPlayback() }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(attachment.name) }, text = {
        androidx.compose.ui.viewinterop.AndroidView(factory = {
            video.apply {
                setVideoPath(attachment.path)
                setMediaController(android.widget.MediaController(context).apply { setAnchorView(video) })
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ -> vm.message("This video format isn't supported by your device."); onDismiss(); true }
            }
        }, modifier = Modifier.fillMaxWidth().height(280.dp))
    }, confirmButton = { TextButton(onDismiss) { Text("Close video") } })
}
