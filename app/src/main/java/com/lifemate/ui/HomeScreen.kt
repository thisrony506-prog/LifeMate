package com.lifemate.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.lifemate.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

val taskKinds = setOf(Kind.ROUTINE, Kind.MISSION, Kind.HABIT, Kind.REMINDER)
@Composable fun HomeScreen(state: LifeState, vm: LifeViewModel, today: LocalDate, navigate: (String) -> Unit, openMenu: () -> Unit = { navigate("menu") }) {
    val tasks = remember(state.items,today) { state.items.filter { it.kind in taskKinds && it.occurs(today) }.sortedBy { it.time } }
    val completed = tasks.count { state.done(it,today) }
    val progress = if(tasks.isEmpty()) 0f else completed.toFloat()/tasks.size
    val animated by animateFloatAsState(progress,label="Today progress")
    val missions = remember(state.items,state.completions) { state.items.filter { it.kind==Kind.MISSION && !it.archived && state.progress(it)<1f }.take(2) }
    val birthday = remember(state.items,today) { state.items.filter { it.kind==Kind.BIRTHDAY && !it.archived }.minByOrNull { Schedule.nextBirthday(LocalDate.parse(it.date),today) } }
    val next = tasks.firstOrNull { it.kind==Kind.ROUTINE && !state.done(it,today) }
    val greeting = when(LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    LazyColumn(Modifier.fillMaxSize().testTag("home-feed"),contentPadding=PaddingValues(20.dp,8.dp,20.dp,96.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                IconButton(openMenu) { Icon(Icons.Outlined.Menu,"Open menu") }
                BrandMark(30.dp)
                Spacer(Modifier.weight(1f))
                IconButton({ navigate("search") }) { Icon(Icons.Outlined.Search,"Search everything") }
                IconButton({ navigate("notifications") }) { Icon(Icons.Outlined.Notifications,"Notifications") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clickable { navigate("profile") }.semantics { contentDescription="Open your profile" }) { Avatar(state.profile,56.dp) }
                Column(Modifier.weight(1f)) {
                    Text(greeting,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.profile?.displayName ?: "LifeMate",style=MaterialTheme.typography.headlineLarge)
                }
                Text(today.format(DateTimeFormatter.ofPattern("d MMM")),style=MaterialTheme.typography.labelLarge)
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Eyebrow("Today's progress"); Text("$completed / ${tasks.size}",style=MaterialTheme.typography.headlineLarge) }
                    Text("${(progress*100).toInt()}%",style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(progress={ animated },modifier=Modifier.fillMaxWidth().height(7.dp),trackColor=MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(Kind.ROUTINE,Kind.MISSION,Kind.REMINDER,Kind.NOTE).forEach { kind ->
                    AssistChip({ navigate("edit/${kind.name}/new") },label={ Text(kind.label) },leadingIcon={ Icon(Icons.Outlined.Add,null,Modifier.size(16.dp)) })
                }
                AssistChip({ navigate("post/new") },label={ Text("Post") },leadingIcon={ Icon(Icons.Outlined.Add,null,Modifier.size(16.dp)) })
            }
        }
        if(next!=null) item { SoftCard(Modifier.fillMaxWidth().clickable { navigate("detail/${next.id}") }) {
            Eyebrow("Next routine")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(LocalTime.parse(next.time).format(timeFormat),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleMedium)
                Text(next.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
            }
        } }
        item { SectionHeading("Today","Calendar") { navigate("calendar") } }
        if(tasks.isEmpty()) item { EmptyState(Kind.ROUTINE,"No tasks today","", "Add routine") { navigate("edit/ROUTINE/new") } }
        items(tasks.take(5),key={it.id}) { item -> ItemRow(item,state,{ navigate("detail/${item.id}") },{vm.toggle(item,today)},today) }
        if(tasks.size>5) item { TextButton({navigate("calendar")}) { Text("All ${tasks.size} tasks") } }
        if(missions.isNotEmpty()) {
            item { SectionHeading("Missions","All") { navigate("missions") } }
            items(missions,key={"mission-${it.id}"}) { item -> ItemRow(item,state,{navigate("detail/${item.id}")}) }
        }
        item { WeeklyOverview(state,today) { navigate("statistics") } }
        if(birthday!=null) item { SoftCard(Modifier.fillMaxWidth().clickable {navigate("detail/${birthday.id}")}) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                KindBadge(Kind.BIRTHDAY)
                Column(Modifier.weight(1f)) { Eyebrow("Upcoming birthday"); Text(birthday.title,style=MaterialTheme.typography.titleMedium) }
                Text(Schedule.nextBirthday(LocalDate.parse(birthday.date),today).format(DateTimeFormatter.ofPattern("d MMM")),style=MaterialTheme.typography.labelLarge)
            }
        } }
        item { OutlinedButton({ navigate("posts") },Modifier.fillMaxWidth()) { Icon(Icons.Outlined.EditNote,null); Text("  Create a post") } }
    }
}
