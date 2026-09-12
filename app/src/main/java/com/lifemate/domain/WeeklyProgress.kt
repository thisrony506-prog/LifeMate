package com.lifemate.domain

import com.lifemate.database.Completion
import com.lifemate.database.LifeItem
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

/** Scheduled occurrences, never invented scores. Future days are not overdue. */
data class WeekProgress(val start: LocalDate, val scheduled: Int, val completed: Int, val due: Int, val doneDue: Int, val missed: Int) {
    val remaining get() = (scheduled - completed).coerceAtLeast(0)
    val rate get() = if (due == 0) 0 else doneDue * 100 / due
    val fraction get() = if (scheduled == 0) 0f else completed.toFloat() / scheduled
}
object WeeklyProgress {
    fun calculate(items: List<LifeItem>, completions: List<Completion>, today: LocalDate): WeekProgress {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val tasks = items.filter { !it.archived && it.kind in setOf(Kind.ROUTINE, Kind.MISSION, Kind.HABIT, Kind.REMINDER) }
        val done = completions.map { it.itemId to it.date }.toSet()
        var scheduled = 0; var completed = 0; var due = 0; var doneDue = 0; var missed = 0
        repeat(7) { offset ->
            val day = start.plusDays(offset.toLong())
            tasks.filter { it.occurs(day) }.forEach { item ->
                scheduled++
                val checked = day <= today && (item.id to day.toString()) in done
                if (checked) completed++
                if (day <= today) { due++; if (checked) doneDue++ }
                if (day < today && !checked) missed++
            }
        }
        return WeekProgress(start, scheduled, completed, due, doneDue, missed)
    }
}
