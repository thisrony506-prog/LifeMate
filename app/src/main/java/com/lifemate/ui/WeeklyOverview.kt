package com.lifemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lifemate.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun WeeklyOverview(state: LifeState, today: LocalDate = LocalDate.now(), onOpen: (() -> Unit)? = null) {
    val week = WeeklyProgress.calculate(state.items, state.completions, today)
    val previous = WeeklyProgress.calculate(state.items, state.completions, today.minusWeeks(1))
    val delta = week.rate - previous.rate
    SoftCard(Modifier.fillMaxWidth().testTag("weekly-overview"), MaterialTheme.colorScheme.surface) {
        Eyebrow("THIS WEEK · ${week.start.format(DateTimeFormatter.ofPattern("d MMM"))} – ${week.start.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))}")
                Text("${week.completed} / ${week.scheduled}", style = MaterialTheme.typography.headlineMedium)
        LinearProgressIndicator(progress = { week.fraction }, modifier = Modifier.fillMaxWidth(), color = featureAccent(Kind.MISSION))
        Text(if (week.scheduled == 0) "No tasks this week" else "${week.remaining} left · ${week.missed} missed")
        Text("${week.rate}% due-to-date", style = MaterialTheme.typography.bodyMedium)
        if (previous.due > 0 && week.due > 0) Text("${if (delta >= 0) "+" else ""}$delta pts vs last week", style = MaterialTheme.typography.bodyMedium)
        onOpen?.let { TextButton(it) { Text("Weekly insights") } }
    }
}
