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
    SoftCard(Modifier.fillMaxWidth().testTag("weekly-overview"), featureContainer(Kind.MISSION)) {
        Eyebrow("THIS WEEK · ${week.start.format(DateTimeFormatter.ofPattern("d MMM"))} – ${week.start.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))}")
        Text("Small steps, a stronger week", style = MaterialTheme.typography.titleLarge)
        Text("${week.completed} / ${week.scheduled} weekly tasks", style = MaterialTheme.typography.headlineMedium)
        LinearProgressIndicator(progress = { week.fraction }, modifier = Modifier.fillMaxWidth(), color = featureAccent(Kind.MISSION))
        Text(if (week.scheduled == 0) "Plan one small mission to begin. Your real progress will appear here." else "${week.remaining} left this week · ${week.missed} missed before today")
        Text("${week.rate}% of tasks due so far completed", style = MaterialTheme.typography.bodyMedium)
        if (previous.due > 0 && week.due > 0) Text("${if (delta >= 0) "+" else ""}$delta percentage points vs the same point last week", style = MaterialTheme.typography.bodyMedium)
        if (week.due > week.doneDue) Text("Next step: complete one due task. Future days are not counted as missed.", style = MaterialTheme.typography.bodyMedium)
        onOpen?.let { TextButton(it) { Text("View weekly insights") } }
    }
}
