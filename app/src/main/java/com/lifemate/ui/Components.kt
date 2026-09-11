package com.lifemate.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.lifemate.database.*
import com.lifemate.domain.*
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter

fun Kind.icon(): ImageVector = when (this) {
    Kind.ROUTINE -> Icons.Outlined.WbSunny; Kind.MISSION -> Icons.Outlined.Flag; Kind.HABIT -> Icons.Outlined.Spa
    Kind.REMINDER -> Icons.Outlined.Notifications; Kind.BIRTHDAY -> Icons.Outlined.Cake; Kind.NOTE -> Icons.Outlined.Description
    Kind.MEMORY -> Icons.Outlined.PhotoLibrary; Kind.GOAL -> Icons.Outlined.TrackChanges
}
fun Kind.tint() = when (this) { Kind.MISSION, Kind.GOAL -> Color(0xFFAA7A4B); Kind.BIRTHDAY -> Color(0xFFAE7186); Kind.NOTE -> Color(0xFF8175AC); Kind.MEMORY -> Color(0xFF628AAC); else -> Jade }
val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
@Composable fun Eyebrow(text: String) { Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable fun PageHeading(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(6.dp)); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        action?.invoke()
    }
}
@Composable fun SectionHeading(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (action != null) TextButton(onClick = onAction) { Text(action); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.padding(start = 6.dp).size(16.dp)) }
    }
}
@Composable fun SoftCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = color, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun KindBadge(kind: Kind, size: Dp = 46.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(15.dp)).background(kind.tint().copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(kind.icon(), null, Modifier.size(size * .48f), tint = kind.tint()) }
}
@Composable fun Avatar(profile: Profile?, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (!profile?.photo.isNullOrBlank()) AsyncImage(File(profile!!.photo), "Profile photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(profile?.displayName?.take(1)?.uppercase() ?: "L", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
    }
}
@Composable fun EmptyState(kind: Kind?, title: String, text: String, action: String? = null, onAction: () -> Unit = {}) {
    SoftCard(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (kind != null) KindBadge(kind, 60.dp) else Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (action != null) Button(onClick = onAction) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(action) }
            }
        }
    }
}
@Composable fun ItemRow(item: LifeItem, state: LifeState, onOpen: () -> Unit, onToggle: (() -> Unit)? = null, day: LocalDate = LocalDate.now()) {
    val done = if (item.kind in setOf(Kind.MISSION, Kind.HABIT)) state.done(item, day) else state.completed(item, day)
    Surface(onClick = onOpen, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KindBadge(item.kind)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (item.pinned) Icon(Icons.Outlined.PushPin, "Pinned", Modifier.padding(start = 4.dp).size(14.dp))
                }
                val subtitle = when (item.kind) {
                    Kind.MISSION -> "${state.dates(item).size} / ${item.duration} days · ${item.dailyTarget.ifBlank { "One day at a time" }}"
                    Kind.BIRTHDAY -> "${Schedule.nextBirthday(LocalDate.parse(item.date), day).format(dateFormat)} · ${item.relationship}"
                    Kind.NOTE, Kind.MEMORY -> item.description.ifBlank { LocalDate.parse(item.date).format(dateFormat) }
                    Kind.GOAL -> "${item.progress}% complete · ${LocalDate.parse(item.date).format(dateFormat)}"
                    else -> "${LocalTime.parse(item.time).format(timeFormat)} · ${if (done) "Completed" else item.repeat.label}"
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.kind in setOf(Kind.MISSION, Kind.GOAL)) LinearProgressIndicator(progress = { state.progress(item) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(5.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
            if (onToggle != null) IconToggleButton(checked = done, onCheckedChange = { onToggle() }) { Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, if (done) "Mark ${item.title} incomplete" else "Complete ${item.title}", tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) }
            else Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Open details", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun ConfirmDialog(title: String, text: String, confirm: String = "Delete", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Outlined.Info, null) }, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
@Composable fun ChoiceChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(selected == option, { onSelect(option) }, label = { Text(option) }) }
    }
}
@Composable fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, shape = RoundedCornerShape(16.dp), singleLine = singleLine, minLines = minLines)
}
