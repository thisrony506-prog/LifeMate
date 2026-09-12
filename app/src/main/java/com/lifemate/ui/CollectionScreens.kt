package com.lifemate.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import com.lifemate.database.*
import com.lifemate.domain.*
import java.io.File
import java.time.*

@Composable fun CollectionScreen(kind: Kind, state: LifeState, vm: LifeViewModel, navigate: (String) -> Unit) {
    var query by rememberSaveable(kind) { mutableStateOf("") }
    var filter by rememberSaveable(kind) { mutableStateOf("All") }
    var sort by rememberSaveable(kind) { mutableStateOf(if (kind == Kind.BIRTHDAY) "Date" else "Newest") }
    val today = LocalDate.now()
    val all = state.items.filter { it.kind == kind }
    val filtered = all.filter { item ->
        (if (filter == "Archived") item.archived else !item.archived) &&
            (query.isBlank() || listOf(item.title, item.description, item.notes, item.tags, item.nickname, item.relationship, item.date).any { it.contains(query, true) }) &&
            when (filter) {
                "Today" -> item.occurs(today)
                "Upcoming" -> if (kind == Kind.BIRTHDAY) true else Schedule.next(item.spec(), Instant.now(), ZoneId.systemDefault()) != null
                "Completed" -> state.completed(item)
                "Pending" -> !state.completed(item)
                "Pinned" -> item.pinned
                "Important" -> item.important
                else -> true
            }
    }.let { records -> when (sort) {
        "Name" -> records.sortedBy { it.title.lowercase() }
        "Date" -> records.sortedBy { if (kind == Kind.BIRTHDAY) Schedule.nextBirthday(LocalDate.parse(it.date), today).toString() else it.date + it.time }
        else -> records.sortedWith(compareByDescending<LifeItem> { it.pinned }.thenByDescending { it.createdAt })
    } }
    val subtitle = when (kind) {
        Kind.MISSION -> "Big changes begin with small commitments."
        Kind.HABIT -> "Build a rhythm that feels like you."
        Kind.MEMORY -> "Keep the moments. Remember the feeling."
        Kind.BIRTHDAY -> "Your favorite people, never forgotten."
        Kind.NOTE -> "A little space for everything on your mind."
        Kind.GOAL -> "A direction for the person you're becoming."
        Kind.ROUTINE -> "Less remembering. More living."
        Kind.REMINDER -> "Make space in your mind. We'll remember."
    }
    LazyColumn(contentPadding = PaddingValues(22.dp, 16.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SoftCard(Modifier.fillMaxWidth(), featureContainer(kind)) { KindBadge(kind); PageHeading(kind.plural, subtitle) } }
        if (kind == Kind.BIRTHDAY) item { BirthdayTheme { Button({ navigate("studio") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Palette, null); Text("  Open photo & card studio") } } }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search names, tags, dates…") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(18.dp), singleLine = true) }
        item { ChoiceChips(if (kind == Kind.NOTE) listOf("All", "Pinned", "Important", "Archived") else if (kind == Kind.MEMORY) listOf("All", "Pinned", "Archived") else if (kind == Kind.BIRTHDAY) listOf("All", "Today", "Upcoming", "Archived") else listOf("All", "Today", "Upcoming", "Pending", "Completed", "Archived"), filter) { filter = it } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("${filtered.size} ${kind.plural}")
            var expanded by remember { mutableStateOf(false) }
            Box { TextButton({ expanded = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp)); Text(" $sort") }; DropdownMenu(expanded, { expanded = false }) { listOf("Newest", "Name", "Date").forEach { option -> DropdownMenuItem({ Text(option) }, { sort = option; expanded = false }) } } }
        } }
        if (filtered.isEmpty()) item {
            val title = if (all.isNotEmpty()) "Nothing here just yet" else when (kind) { Kind.ROUTINE -> "Create your first routine"; Kind.MISSION -> "Start your first mission"; Kind.BIRTHDAY -> "Add an important person's birthday"; else -> "Your ${kind.plural.lowercase()} start here" }
            EmptyState(kind, title, if (all.isNotEmpty()) "Try another search or filter." else subtitle, "New ${kind.label.lowercase()}") { navigate("edit/${kind.name}/new") }
        }
        items(filtered, key = { it.id }) { item ->
            if (kind == Kind.MEMORY) {
                val image = state.media(item).firstOrNull { it.mime.startsWith("image/") }
                Surface(onClick = { navigate("detail/${item.id}") }, shape = RoundedCornerShape(24.dp)) {
                    Column {
                        if (image != null) AsyncImage(File(image.path), item.title, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
                        else Box(Modifier.fillMaxWidth().height(130.dp).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(if (state.media(item).any { it.mime.startsWith("video/") }) Icons.Outlined.PlayCircle else Icons.Outlined.PhotoCamera, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary) }
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Eyebrow(LocalDate.parse(item.date).format(dateFormat)); Text(item.title, style = MaterialTheme.typography.titleLarge); if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            } else ItemRow(item, state, { navigate("detail/${item.id}") }, if (kind in taskKinds && item.occurs(today)) ({ vm.toggle(item) }) else null)
        }
    }
}
@Composable fun SearchScreen(state: LifeState, navigate: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val results = state.items.filter { !it.archived && query.isNotBlank() && (category == "All" || category == it.kind.label) && listOf(it.title, it.description, it.notes, it.tags, it.nickname, it.relationship, it.date).any { value -> value.contains(query, true) } }.groupBy { it.kind }
    LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeading("Find your things", "One search. Your whole LifeMate.") }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text("Name, person, note, tag, or date") }, shape = RoundedCornerShape(18.dp), singleLine = true) }
        item { ChoiceChips(listOf("All") + Kind.entries.map { it.label }, category) { category = it } }
        if (results.isEmpty()) item { EmptyState(null, if (query.isBlank()) "What are you looking for?" else "No matches found", "Search routines, missions, habits, reminders, people, notes, memories, and goals.") }
        results.forEach { (kind, items) -> item { SectionHeading(kind.plural) }; items(items, key = { it.id }) { ItemRow(it, state, { navigate("detail/${it.id}") }) } }
    }
}
@Composable fun MenuScreen(navigate: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Your life, organized", "Everything you need, in one gentle space.") }
        listOf("Plan your day" to listOf(Kind.ROUTINE, Kind.REMINDER), "Grow with intention" to listOf(Kind.MISSION, Kind.HABIT, Kind.GOAL), "Keep & celebrate" to listOf(Kind.BIRTHDAY, Kind.MEMORY, Kind.NOTE)).forEach { (title, kinds) ->
        item { SectionHeading(title) }
        items(kinds) { kind ->
            Surface(onClick = { navigate("list/${kind.name}") }, modifier = Modifier.testTag("feature-${kind.name}"), shape = RoundedCornerShape(22.dp), color = featureContainer(kind),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    KindBadge(kind, 48.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(kind.plural, style = MaterialTheme.typography.titleMedium)
                        Text(kind.summary(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        }
        item { BirthdayTheme { Button({ navigate("studio") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Palette, null); Text("  Photo & card studio") } } }
        item { OutlinedButton({ navigate("statistics") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Insights, null); Text("  Your statistics") } }
        item { OutlinedButton({ navigate("settings") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Settings, null); Text("  Settings & privacy") } }
    }
}
