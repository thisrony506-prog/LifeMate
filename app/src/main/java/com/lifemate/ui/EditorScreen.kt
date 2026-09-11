package com.lifemate.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.lifemate.database.*
import com.lifemate.domain.*
import org.json.*
import java.time.*

@Composable fun DateField(label: String, value: String, optional: Boolean = false, onChange: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val date = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
        DatePickerDialog(context, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = RoundedCornerShape(16.dp)) {
        Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(12.dp)); Text("$label: ${value.ifBlank { "Not set (optional)" }}", Modifier.weight(1f))
    }
    if (optional && value.isNotBlank()) TextButton({ onChange("") }) { Text("Clear date") }
}
@Composable fun TimeField(value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val time = LocalTime.parse(value)
        TimePickerDialog(context, { _, h, m -> onChange(LocalTime.of(h, m).toString()) }, time.hour, time.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = RoundedCornerShape(16.dp)) {
        Icon(Icons.Outlined.Schedule, null); Spacer(Modifier.width(12.dp)); Text("Reminder time: ${LocalTime.parse(value).format(timeFormat)}", Modifier.weight(1f))
    }
}
@Composable fun ToggleRow(title: String, subtitle: String = "", checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked, onCheckedChange = null)
    }
}
@Composable fun SoundPicker(sound: String, onChange: (String) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            onChange(uri?.toString() ?: "silent")
        }
    }
    val context = LocalContext.current
    ChoiceChips(listOf("Default", "Silent", "Custom"), if (sound == "default") "Default" else if (sound == "silent") "Silent" else "Custom") {
        when (it) {
            "Default" -> onChange("default")
            "Silent" -> onChange("silent")
            else -> try { picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION).putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true).putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (sound.startsWith("content:")) Uri.parse(sound) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))) }
            catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context, "No sound picker is available on this device.", android.widget.Toast.LENGTH_LONG).show() }
        }
    }
}
fun checklistLines(json: String): String = runCatching { JSONArray(json).let { a -> (0 until a.length()).joinToString("\n") { a.getJSONObject(it).getString("text") } } }.getOrDefault("")
fun checklistJson(lines: String, original: String): String {
    val old = runCatching { JSONArray(original) }.getOrDefault(JSONArray())
    val checks = (0 until old.length()).associate { old.getJSONObject(it).getString("text") to old.getJSONObject(it).optBoolean("done") }
    return JSONArray(lines.lines().map(String::trim).filter(String::isNotBlank).map { JSONObject().put("text", it).put("done", checks[it] ?: false) }).toString()
}
@Composable fun EditorScreen(kind: Kind, original: LifeItem?, initialDate: String?, state: LifeState, vm: LifeViewModel, onSaved: (String) -> Unit) {
    var draft by rememberSaveable(original?.id, kind) { mutableStateOf(original ?: LifeItem(kind = kind, date = initialDate ?: LocalDate.now().toString(), repeat = if (kind in setOf(Kind.ROUTINE, Kind.HABIT)) Repeat.DAILY else Repeat.ONCE, sound = "default", vibration = state.preferences.vibration, notifications = kind !in setOf(Kind.NOTE, Kind.MEMORY))) }
    var duration by rememberSaveable { mutableStateOf(draft.duration.toString()) }
    var interval by rememberSaveable { mutableStateOf(draft.interval.toString()) }
    var checklist by rememberSaveable { mutableStateOf(checklistLines(draft.checklist)) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val busy by vm.busy.collectAsState()
    val scheduled = kind !in setOf(Kind.NOTE, Kind.MEMORY)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        KindBadge(kind, 56.dp)
        PageHeading("${if (original == null) "New" else "Edit"} ${kind.label.lowercase()}", when (kind) {
            Kind.MISSION -> "Make a promise to yourself. Start small."
            Kind.BIRTHDAY -> "Remember the people who matter."
            Kind.NOTE -> "Get it out of your head, into a safe space."
            Kind.MEMORY -> "Some moments deserve to stay."
            else -> "A little intention goes a long way."
        })
        Field(draft.title, { draft = draft.copy(title = it.take(200)) }, if (kind == Kind.BIRTHDAY) "Person's full name *" else "${kind.label} name *")
        if (kind == Kind.BIRTHDAY) {
            Field(draft.nickname, { draft = draft.copy(nickname = it) }, "Nickname (optional)")
            Text("Relationship", style = MaterialTheme.typography.titleMedium)
            ChoiceChips(listOf("Friend", "Family", "Partner", "Colleague", "Other"), draft.relationship) { draft = draft.copy(relationship = it) }
            Field(draft.relationship, { draft = draft.copy(relationship = it) }, "Relationship / category")
        }
        Field(draft.description, { draft = draft.copy(description = it) }, when (kind) { Kind.NOTE -> "Your note"; Kind.MEMORY -> "Caption / story"; Kind.BIRTHDAY -> "Personal note"; else -> "Description (optional)" }, singleLine = false, minLines = if (kind == Kind.NOTE) 5 else 2)
        DateField(when (kind) { Kind.BIRTHDAY -> "Birthday"; Kind.GOAL -> "Target date"; Kind.NOTE, Kind.MEMORY -> "Date"; else -> "Start date" }, draft.date) { draft = draft.copy(date = it) }
        if (kind == Kind.MISSION) {
            SectionHeading("How long is your journey?")
            ChoiceChips(listOf("1", "2", "3", "7", "30"), duration) { duration = it }
            OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("Duration in days (1–36500)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp))
            Field(draft.dailyTarget, { draft = draft.copy(dailyTarget = it) }, "Daily target, e.g. Read 10 pages")
        }
        if (kind == Kind.GOAL) {
            Text("Progress: ${draft.progress}%", style = MaterialTheme.typography.titleMedium)
            Slider(draft.progress.toFloat(), { draft = draft.copy(progress = it.toInt()) }, valueRange = 0f..100f, steps = 99)
            Field(checklist, { checklist = it }, "Milestones · one per line", singleLine = false, minLines = 3)
            ChoiceChips(listOf("Short-term", "Long-term"), draft.tags.ifBlank { "Short-term" }) { draft = draft.copy(tags = it) }
        }
        if (kind == Kind.NOTE) Field(checklist, { checklist = it }, "Checklist · one item per line (optional)", singleLine = false, minLines = 3)
        if (kind !in setOf(Kind.MISSION, Kind.BIRTHDAY, Kind.NOTE, Kind.MEMORY, Kind.GOAL)) {
            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            ChoiceChips(Repeat.entries.map { it.label }, draft.repeat.label) { label -> draft = draft.copy(repeat = Repeat.entries.first { it.label == label }) }
            if (draft.repeat == Repeat.SPECIFIC) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val days = draft.spec().days
                    FilterChip(day.value in days, { draft = draft.copy(weekdays = (if (day.value in days) days - day.value else days + day.value).sorted().joinToString(",")) }, label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) })
                }
            }
            if (draft.repeat == Repeat.CUSTOM) OutlinedTextField(interval, { interval = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Repeat every N days (1–1461)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp))
        }
        if (scheduled) {
            ToggleRow("Remind me", "A gentle nudge, right when you need it.", draft.notifications) { draft = draft.copy(notifications = it) }
            if (draft.notifications) {
                TimeField(draft.time) { draft = draft.copy(time = it) }
                if (kind !in setOf(Kind.MISSION, Kind.BIRTHDAY) && draft.repeat == Repeat.ONCE && LocalDateTime.of(LocalDate.parse(draft.date), LocalTime.parse(draft.time)) <= LocalDateTime.now()) {
                    Text("This time has passed. You can save it for your records, but choose a future time to receive a notification.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (kind == Kind.BIRTHDAY) {
                    Text("Remind me before their birthday")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7, 3, 1, 0).forEach { offset -> val selected = draft.spec().birthdayOffsets; FilterChip(offset in selected, { draft = draft.copy(birthdayOffsets = (if (offset in selected) selected - offset else selected + offset).sortedDescending().joinToString(",")) }, label = { Text(if (offset == 0) "On the day" else "$offset days") }) }
                    }
                }
                if (!vm.app.scheduler.permissionGranted()) Text("Notifications aren't allowed yet. Enable them in Settings to receive reminders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                TextButton({ advanced = !advanced }) { Icon(Icons.Outlined.Tune, null); Text("  ${if (advanced) "Hide" else "Customize"} notification options") }
                if (advanced) {
                    Field(draft.notificationText, { draft = draft.copy(notificationText = it) }, "Custom notification message (optional)", singleLine = false)
                    Text("Notification sound"); SoundPicker(draft.sound) { draft = draft.copy(sound = it) }
                    ToggleRow("Vibration", checked = draft.vibration) { draft = draft.copy(vibration = it) }
                    ToggleRow("Important", "Use a high-priority notification. Android controls heads-up display.", draft.important) { draft = draft.copy(important = it) }
                }
            }
        }
        if (kind !in setOf(Kind.NOTE, Kind.BIRTHDAY)) Field(draft.notes, { draft = draft.copy(notes = it) }, "Personal notes (optional)", singleLine = false, minLines = 2)
        if (kind != Kind.GOAL) Field(draft.tags, { draft = draft.copy(tags = it) }, "Tags / category, separated by commas")
        if (kind == Kind.NOTE) ToggleRow("Important note", checked = draft.important) { draft = draft.copy(important = it) }
        SoftCard(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Outlined.AttachFile, null); Text("After saving, add photos, videos, or a voice recording from the details screen. Files stay on your device.", style = MaterialTheme.typography.bodyMedium) }
        }
        Button(onClick = {
            val d = duration.toIntOrNull(); val n = interval.toIntOrNull()
            when {
                draft.title.isBlank() -> vm.message("Please add a name.")
                d == null || d !in 1..36500 -> vm.message("Enter a duration between 1 and 36500 days.")
                n == null || n !in 1..1461 -> vm.message("Enter a repeat interval between 1 and 1461 days.")
                draft.kind == Kind.BIRTHDAY && draft.notifications && draft.spec().birthdayOffsets.isEmpty() -> vm.message("Choose at least one birthday reminder, or turn reminders off.")
                else -> vm.save(draft.copy(title = draft.title.trim(), duration = d, interval = n, checklist = checklistJson(checklist, draft.checklist))) { onSaved(draft.id) }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) { if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else { Icon(Icons.Outlined.Check, null); Text("  Save ${kind.label.lowercase()}") } }
        Spacer(Modifier.height(24.dp))
    }
}
