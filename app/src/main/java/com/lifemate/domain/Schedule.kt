package com.lifemate.domain

import java.time.*
import java.time.temporal.ChronoUnit

enum class Kind(val label: String, val plural: String) {
    ROUTINE("Routine", "Routines"), MISSION("Mission", "Missions"), HABIT("Habit", "Habits"),
    REMINDER("Reminder", "Reminders"), BIRTHDAY("Birthday", "Birthdays"), NOTE("Note", "Notes"),
    MEMORY("Memory", "Memories"), GOAL("Goal", "Goals")
}
enum class Repeat(val label: String) {
    ONCE("Once"), DAILY("Every day"), WEEKDAYS("Weekdays"), WEEKENDS("Weekends"),
    SPECIFIC("Specific days"), WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly"), CUSTOM("Every N days")
}
data class ScheduleSpec(
    val kind: Kind, val start: LocalDate, val time: LocalTime = LocalTime.of(9, 0),
    val repeat: Repeat = Repeat.ONCE, val days: Set<Int> = emptySet(), val interval: Int = 1,
    val duration: Int = 30, val birthdayOffsets: Set<Int> = setOf(7, 3, 1, 0)
)
object Schedule {
    fun birthdayInYear(date: LocalDate, year: Int): LocalDate =
        LocalDate.of(year, date.month, date.dayOfMonth.coerceAtMost(YearMonth.of(year, date.month).lengthOfMonth()))

    fun occurs(spec: ScheduleSpec, day: LocalDate): Boolean {
        if (spec.kind == Kind.BIRTHDAY) return birthdayInYear(spec.start, day.year) == day
        if (day < spec.start) return false
        if (spec.kind == Kind.MISSION) return ChronoUnit.DAYS.between(spec.start, day) < spec.duration
        if (spec.kind == Kind.NOTE || spec.kind == Kind.MEMORY) return spec.start == day
        return when (spec.repeat) {
            Repeat.ONCE -> day == spec.start
            Repeat.DAILY -> true
            Repeat.WEEKDAYS -> day.dayOfWeek.value <= 5
            Repeat.WEEKENDS -> day.dayOfWeek.value >= 6
            Repeat.SPECIFIC -> day.dayOfWeek.value in spec.days
            Repeat.WEEKLY -> day.dayOfWeek == spec.start.dayOfWeek
            Repeat.MONTHLY -> day.dayOfMonth == spec.start.dayOfMonth.coerceAtMost(YearMonth.from(day).lengthOfMonth())
            Repeat.YEARLY -> birthdayInYear(spec.start, day.year) == day
            Repeat.CUSTOM -> ChronoUnit.DAYS.between(spec.start, day) % spec.interval.coerceAtLeast(1) == 0L
        }
    }
    /** Wall-clock schedules follow the current zone. Gaps shift forward; overlaps use the first offset. */
    fun next(spec: ScheduleSpec, after: Instant, zone: ZoneId): Instant? {
        if (spec.kind == Kind.NOTE || spec.kind == Kind.MEMORY) return null
        val today = after.atZone(zone).toLocalDate()
        if (spec.kind == Kind.BIRTHDAY) {
            return (today.year..today.year + 2).flatMap { year ->
                spec.birthdayOffsets.map { offset -> birthdayInYear(spec.start, year).minusDays(offset.toLong()).atTime(spec.time).atZone(zone).toInstant() }
            }.filter { it > after }.minOrNull()
        }
        // Start near the anchor so future one-time reminders are not bounded by today's horizon.
        val beginning = maxOf(today, spec.start)
        return (0L..1461L).asSequence().map { beginning.plusDays(it) }
            .filter { occurs(spec, it) }.map { it.atTime(spec.time).atZone(zone).toInstant() }
            .firstOrNull { it > after }
    }
    fun nextBirthday(date: LocalDate, today: LocalDate): LocalDate =
        birthdayInYear(date, today.year).let { if (it < today) birthdayInYear(date, today.year + 1) else it }

    fun currentStreak(dates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in dates) today else today.minusDays(1)
        var count = 0
        while (cursor in dates) { count++; cursor = cursor.minusDays(1) }
        return count
    }
    fun bestStreak(dates: Set<LocalDate>): Int {
        var best = 0; var count = 0; var last: LocalDate? = null
        dates.sorted().forEach { day -> count = if (last?.plusDays(1) == day) count + 1 else 1; best = maxOf(best, count); last = day }
        return best
    }
}
