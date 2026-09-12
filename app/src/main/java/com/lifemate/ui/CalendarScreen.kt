package com.lifemate.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifemate.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun CalendarScreen(state: LifeState, vm: LifeViewModel, navigate: (String) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(monthText); val selected = LocalDate.parse(selectedText)
    val calendarKinds = taskKinds + setOf(Kind.BIRTHDAY, Kind.GOAL, Kind.MEMORY)
    val records = remember(state.items) { state.items.filter { it.kind in calendarKinds && !it.archived } }
    val indicators = remember(records,month) { val start=month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value-1).toLong()); (0L..41L).associate { offset -> val day=start.plusDays(offset); day to records.filter { it.occurs(day) }.map { it.kind }.distinct() } }
    val events = records.filter { it.occurs(selected) }.sortedBy { it.time }
    LazyColumn(contentPadding = PaddingValues(22.dp, 16.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("Calendar", "") }
        item { SoftCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton({ monthText = month.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month") }
                IconButton({ monthText = month.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month") }
            }
            Row { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            val start = month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value - 1).toLong())
            (0..5).forEach { week -> Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { col ->
                    val day = start.plusDays((week * 7 + col).toLong())
                    val dayEvents = indicators[day].orEmpty()
                    Column(Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape).background(if (day == selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedText = day.toString() }.semantics { contentDescription = "${day.format(dateFormat)}, ${dayEvents.size} event types" }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.dayOfMonth.toString(), color = if (YearMonth.from(day) != month) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f) else MaterialTheme.colorScheme.onSurface)
                        Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { dayEvents.take(3).forEach { kind -> Box(Modifier.padding(top = 3.dp).size(4.dp).clip(CircleShape).background(kind.tint())) } }
                    }
                }
            } }
            TextButton({ selectedText = LocalDate.now().toString(); monthText = YearMonth.now().toString() }, Modifier.align(Alignment.CenterHorizontally)) { Text("Back to today") }
        } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) { calendarKinds.forEach { kind -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(Modifier.size(6.dp).clip(CircleShape).background(kind.tint())); Text(kind.label, fontSize = 10.sp) } } } }
        item { SectionHeading(if (selected == LocalDate.now()) "Today" else selected.format(dateFormat), "Add event") { navigate("edit/REMINDER/new?date=$selected") } }
        if (events.isEmpty()) item { EmptyState(null, "No events", "") }
        items(events, key = { it.id }) { item -> ItemRow(item, state, { navigate("detail/${item.id}") }, if (item.kind in taskKinds && selected <= LocalDate.now()) ({ vm.toggle(item, selected) }) else null, selected) }
    }
}
