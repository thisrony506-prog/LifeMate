package com.lifemate.ui

import androidx.compose.foundation.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import com.lifemate.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

val taskKinds = setOf(Kind.ROUTINE, Kind.MISSION, Kind.HABIT, Kind.REMINDER)
@Composable fun HomeScreen(state: LifeState, vm: LifeViewModel, today: LocalDate, navigate: (String) -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 360
    val ringSize = if (compact) 80.dp else 96.dp
    val tasks = state.items.filter { it.kind in taskKinds && it.occurs(today) }.sortedBy { it.time }
    val completed = tasks.count { state.done(it, today) }
    val progress = if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "Daily progress")
    val missions = state.items.filter { it.kind == Kind.MISSION && !it.archived && state.progress(it) < 1f }.take(2)
    val birthday = state.items.filter { it.kind == Kind.BIRTHDAY && !it.archived }.minByOrNull { Schedule.nextBirthday(LocalDate.parse(it.date), today) }
    val comingUp = state.items.filter { !it.archived && it.kind in setOf(Kind.REMINDER, Kind.GOAL) && !state.completed(it) }
        .mapNotNull { item -> Schedule.next(item.spec(), Instant.now(), ZoneId.systemDefault())?.let { item to it } }
        .filter { it.second.atZone(ZoneId.systemDefault()).toLocalDate() > today }.sortedBy { it.second }.take(3)
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 20.dp)) {
        item {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BrandMark(40.dp)
                    Text("LifeMate", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { navigate("search") }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, "Search everything") }
                    IconButton(onClick = { navigate("notifications") }) { Icon(Icons.Outlined.Notifications, "Notifications") }
                    IconButton(onClick = { navigate("settings") }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Settings, "Settings") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Eyebrow(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))) }
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable { navigate("profile") }.semantics { contentDescription = "Open your profile" }, contentAlignment = Alignment.Center) { Avatar(state.profile, 36.dp) }
                }
                Text("$greeting,\n${state.profile?.displayName ?: "friend"}.", style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge)
                if (!compact) Text("A little intention. A little progress. A better you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = Jade, modifier = Modifier.fillMaxWidth()) {
                Box {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(alpha = .035f), size.width * .40f, androidx.compose.ui.geometry.Offset(size.width, 0f))
                        drawCircle(Color.White.copy(alpha = .045f), size.width * .27f, androidx.compose.ui.geometry.Offset(size.width, 0f))
                    }
                    Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("TODAY'S PROGRESS", fontSize = 10.sp, letterSpacing = 1.5.sp, color = Lime)
                            Text(if (tasks.isNotEmpty() && completed == tasks.size) "You did it!" else if (completed == 0) "Make today\na little better." else "Small steps.\nReal progress.", style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, color = Color.White)
                            Text("$completed of ${tasks.size} tasks completed", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD4E5D9))
                        }
                        Box(Modifier.size(ringSize).semantics { contentDescription = "Today's progress ${(progress * 100).toInt()} percent" }, contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawArc(Color.White.copy(alpha = .14f), -90f, 360f, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                                if (animatedProgress > 0) drawArc(Lime, -90f, 360 * animatedProgress, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${(progress * 100).toInt()}%", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold); Text("of today", color = Lime, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
        item { WeeklyOverview(state, today) { navigate("statistics") } }
        item { SectionHeading("Your day", "Calendar") { navigate("calendar") } }
        if (tasks.isEmpty()) item { EmptyState(Kind.ROUTINE, "Create your first routine", "Give your day a little rhythm. Start with one thing that matters.", "Create routine") { navigate("edit/ROUTINE/new") } }
        else {
            if (completed == tasks.size) item { SoftCard(color = MaterialTheme.colorScheme.primaryContainer) { Text("Great job, ${state.profile?.displayName}! You completed everything for today.", style = MaterialTheme.typography.titleMedium) } }
            items(tasks.take(5), key = { it.id }) { item -> ItemRow(item, state, { navigate("detail/${item.id}") }, { vm.toggle(item, today) }, today) }
            if (tasks.size > 5) item { TextButton({ navigate("calendar") }, Modifier.fillMaxWidth()) { Text("View all ${tasks.size} tasks for today") } }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Create something good")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Kind.ROUTINE, Kind.MISSION, Kind.REMINDER, Kind.NOTE).forEach { kind ->
                        Surface(onClick = { navigate("edit/${kind.name}/new") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = featureContainer(kind), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Icon(kind.icon(), null, tint = kind.tint(), modifier = Modifier.size(24.dp)); Text(kind.label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        item { BirthdayTheme { SoftCard(Modifier.fillMaxWidth(), featureContainer(Kind.BIRTHDAY)) {
            Text("Create. Celebrate. Share.", style = MaterialTheme.typography.titleLarge)
            Text("Pink birthday wishes & beautiful photo posts. Made privately, by you.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ navigate("list/BIRTHDAY") }) { Text("Birthdays") }
                Button({ navigate("studio") }) { Text("Photo studio") }
            }
        } } }
        if (birthday != null) item {
            SoftCard(Modifier.fillMaxWidth().clickable { navigate("detail/${birthday.id}") }, color = featureContainer(Kind.BIRTHDAY)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KindBadge(Kind.BIRTHDAY)
                    Column(Modifier.weight(1f)) { Eyebrow("A day worth remembering"); Spacer(Modifier.height(5.dp)); Text("${birthday.title}'s birthday", style = MaterialTheme.typography.titleMedium); Text(Schedule.nextBirthday(LocalDate.parse(birthday.date), today).format(dateFormat), style = MaterialTheme.typography.bodyMedium) }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, "View birthday")
                }
            }
        }
        if (comingUp.isNotEmpty()) {
            item { SectionHeading("Coming up", "Reminders") { navigate("list/REMINDER") } }
            items(comingUp, key = { "upcoming-${it.first.id}" }) { (item, next) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow(next.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat))
                    ItemRow(item, state, { navigate("detail/${item.id}") })
                }
            }
        }
        item { SectionHeading("Growing, one day at a time", "Missions") { navigate("list/MISSION") } }
        if (missions.isEmpty()) item { EmptyState(Kind.MISSION, "Start your first mission", "Seven days or thirty. Your next chapter starts with a small commitment.", "Start a mission") { navigate("edit/MISSION/new") } }
        else items(missions, key = { "mission-${it.id}" }) { item -> ItemRow(item, state, { navigate("detail/${item.id}") }) }
        item {
            TextButton(onClick = { navigate("menu") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.GridView, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Explore your LifeMate") }
            Text("A little better, every day.", Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
