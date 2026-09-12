package com.lifemate.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.lifemate.domain.*
import java.time.*

@Composable fun StatisticsScreen(state: LifeState) {
    var period by rememberSaveable { mutableStateOf("Week") }
    val today = LocalDate.now(); val days = if (period == "Week") 7 else today.dayOfMonth
    val records = state.items.filter { it.kind in taskKinds && !it.archived }
    val dates = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    fun rate(day: LocalDate): Float { val due = records.filter { it.occurs(day) }; return if (due.isEmpty()) 0f else due.count { state.done(it, day) }.toFloat() / due.size }
    val denominator = dates.sumOf { day -> records.count { it.occurs(day) } }
    val done = dates.sumOf { day -> records.count { it.occurs(day) && state.done(it, day) } }
    val percentage = if (denominator == 0) 0 else done * 100 / denominator
    LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("Look how far you've come", "Progress, not perfection.") }
        item { WeeklyOverview(state, today) }
        item { ChoiceChips(listOf("Week", "Month"), period) { period = it } }
        item { SoftCard(Modifier.fillMaxWidth(), MaterialTheme.colorScheme.primaryContainer) {
            Eyebrow("${if (period == "Week") "Last 7 days" else "This month"} · completion rate")
            Text("$percentage%", style = MaterialTheme.typography.displaySmall)
            Text("$done of $denominator scheduled tasks completed")
            LinearProgressIndicator(progress = { percentage / 100f }, modifier = Modifier.fillMaxWidth())
        } }
        item { SoftCard {
            SectionHeading("Your daily rhythm")
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(if (days == 7) 10.dp else 3.dp), verticalAlignment = Alignment.Bottom) {
                dates.forEach { day -> Column(Modifier.weight(1f).semantics { contentDescription = "$day: ${(rate(day) * 100).toInt()} percent completed" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height((110 * rate(day) + 4).dp).clip(RoundedCornerShape(6.dp)).background(if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer))
                    if (days == 7) Text(day.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelSmall)
                } }
            }
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftCard(Modifier.weight(1f)) { Eyebrow("All-time tasks"); Text(state.completions.size.toString(), style = MaterialTheme.typography.headlineLarge); Text("completed", style = MaterialTheme.typography.bodyMedium) }
            SoftCard(Modifier.weight(1f)) { Eyebrow("Active missions"); Text(state.items.count { it.kind == Kind.MISSION && it.occurs(today) && state.progress(it) < 1f }.toString(), style = MaterialTheme.typography.headlineLarge); Text("in progress", style = MaterialTheme.typography.bodyMedium) }
        } }
        item { SoftCard {
            SectionHeading("The bigger picture")
            val routines = records.filter { it.kind == Kind.ROUTINE }
            val total = dates.sumOf { day -> routines.count { it.occurs(day) } }
            val complete = dates.sumOf { day -> routines.count { it.occurs(day) && state.done(it, day) } }
            Text("Routine consistency: ${if (total == 0) 0 else complete * 100 / total}%")
            Text("Completed missions: ${state.items.count { it.kind == Kind.MISSION && state.progress(it) >= 1f }}")
            Text("Birthdays in the next 30 days: ${state.items.count { it.kind == Kind.BIRTHDAY && !it.archived && Schedule.nextBirthday(LocalDate.parse(it.date), today) <= today.plusDays(30) }}")
        } }
        item { SectionHeading("Your weekly missions") }
        val missions = state.items.filter { it.kind == Kind.MISSION && !it.archived }
        missions.forEach { mission -> item { SoftCard(Modifier.fillMaxWidth(), featureContainer(Kind.MISSION)) {
            val week = WeeklyProgress.calculate(listOf(mission), state.completions, today)
            Text(mission.title, style = MaterialTheme.typography.titleMedium)
            Text("${week.completed}/${week.scheduled} this week · ${week.remaining} remaining")
            LinearProgressIndicator(progress = { state.progress(mission) }, modifier = Modifier.fillMaxWidth(), color = featureAccent(Kind.MISSION))
            Text("${(state.progress(mission) * 100).toInt()}% of the full ${mission.duration}-day mission")
            if (mission.dailyTarget.isNotBlank()) Text("Daily focus: ${mission.dailyTarget}")
        } } }
        item { SectionHeading("Habit streaks") }
        val habits = state.items.filter { it.kind == Kind.HABIT && !it.archived }
        if (habits.isEmpty()) item { EmptyState(Kind.HABIT, "Every streak starts at one", "Create a habit to start seeing your progress here.") }
        habits.forEach { habit -> item { SoftCard(Modifier.fillMaxWidth()) {
            Text(habit.title, style = MaterialTheme.typography.titleMedium)
            Text("${Schedule.currentStreak(state.dates(habit), today)} day current streak · ${Schedule.bestStreak(state.dates(habit))} day best")
        } } }
        item { Text("Statistics reflect your current records. Deleted records are removed from totals. Streaks count consecutive calendar days.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
