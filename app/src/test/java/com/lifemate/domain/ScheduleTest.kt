package com.lifemate.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ScheduleTest {
    private val utc = ZoneId.of("UTC")
    private fun spec(repeat: Repeat = Repeat.DAILY, date: String = "2026-09-11", kind: Kind = Kind.ROUTINE) = ScheduleSpec(kind, LocalDate.parse(date), LocalTime.of(9, 0), repeat)
    @Test fun dailyFindsNextDayAfterDueTime() { assertEquals(Instant.parse("2026-09-12T09:00:00Z"), Schedule.next(spec(), Instant.parse("2026-09-11T10:00:00Z"), utc)) }
    @Test fun onceNeverRepeats() { assertNull(Schedule.next(spec(Repeat.ONCE), Instant.parse("2026-09-11T09:00:00Z"), utc)) }
    @Test fun onceFarInFutureStillSchedules() { assertEquals(Instant.parse("2040-01-01T09:00:00Z"), Schedule.next(spec(Repeat.ONCE, "2040-01-01"), Instant.parse("2026-01-01T00:00:00Z"), utc)) }
    @Test fun weekendsSkipWeekdays() { assertFalse(Schedule.occurs(spec(Repeat.WEEKENDS), LocalDate.parse("2026-09-11"))); assertTrue(Schedule.occurs(spec(Repeat.WEEKENDS), LocalDate.parse("2026-09-12"))) }
    @Test fun weekdaysSkipWeekend() { assertFalse(Schedule.occurs(spec(Repeat.WEEKDAYS), LocalDate.parse("2026-09-13"))) }
    @Test fun monthlyClampsAndReturnsToAnchor() { val s = spec(Repeat.MONTHLY, "2026-01-31"); assertTrue(Schedule.occurs(s, LocalDate.parse("2026-02-28"))); assertTrue(Schedule.occurs(s, LocalDate.parse("2026-03-31"))); assertFalse(Schedule.occurs(s, LocalDate.parse("2026-03-28"))) }
    @Test fun leapDayBirthdayObservedOnFebruary28() { assertEquals(LocalDate.parse("2027-02-28"), Schedule.nextBirthday(LocalDate.parse("2000-02-29"), LocalDate.parse("2026-03-01"))) }
    @Test fun birthdayOffsetsCrossYearBoundary() { val s = spec(date = "2000-01-03", kind = Kind.BIRTHDAY); assertEquals(Instant.parse("2026-12-27T09:00:00Z"), Schedule.next(s, Instant.parse("2026-12-25T00:00:00Z"), utc)) }
    @Test fun birthdayAllFourOffsets() { val s = spec(date = "2000-09-15", kind = Kind.BIRTHDAY); var instant = Instant.parse("2026-09-01T00:00:00Z"); val days = listOf(8, 12, 14, 15); days.forEach { day -> instant = Schedule.next(s, instant, utc)!!; assertEquals(day, instant.atZone(utc).dayOfMonth) } }
    @Test fun missionDurationHasExactBoundaries() { val s = spec(kind = Kind.MISSION).copy(duration = 3); assertFalse(Schedule.occurs(s, s.start.minusDays(1))); assertTrue(Schedule.occurs(s, s.start.plusDays(2))); assertFalse(Schedule.occurs(s, s.start.plusDays(3))) }
    @Test fun completedMissionHasNoFutureAlarm() { assertNull(Schedule.next(spec(kind = Kind.MISSION).copy(duration = 3), Instant.parse("2026-09-14T00:00:00Z"), utc)) }
    @Test fun customIntervalUsesAnchor() { val s = spec(Repeat.CUSTOM).copy(interval = 3); assertTrue(Schedule.occurs(s, s.start.plusDays(6))); assertFalse(Schedule.occurs(s, s.start.plusDays(4))) }
    @Test fun specificWeekdays() { val s = spec(Repeat.SPECIFIC).copy(days = setOf(1, 4)); assertTrue(Schedule.occurs(s, LocalDate.parse("2026-09-14"))); assertFalse(Schedule.occurs(s, LocalDate.parse("2026-09-15"))) }
    @Test fun daylightSavingGapShiftsForward() { val s = spec(date = "2026-03-08").copy(time = LocalTime.of(2, 30)); assertEquals(Instant.parse("2026-03-08T07:30:00Z"), Schedule.next(s, Instant.parse("2026-03-08T00:00:00Z"), ZoneId.of("America/New_York"))) }
    @Test fun daylightSavingOverlapUsesFirstOccurrence() { val s = spec(date = "2026-11-01").copy(time = LocalTime.of(1, 30)); assertEquals(Instant.parse("2026-11-01T05:30:00Z"), Schedule.next(s, Instant.parse("2026-11-01T00:00:00Z"), ZoneId.of("America/New_York"))) }
    @Test fun timezoneChangesKeepWallClock() { val s = spec(); assertEquals(9, Schedule.next(s, Instant.parse("2026-09-11T00:00:00Z"), ZoneId.of("Asia/Dhaka"))!!.atZone(ZoneId.of("Asia/Dhaka")).hour) }
    @Test fun bestAndCurrentStreak() { val today = LocalDate.parse("2026-09-11"); val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(4)); assertEquals(2, Schedule.currentStreak(dates, today)); assertEquals(2, Schedule.bestStreak(dates)); assertEquals(0, Schedule.currentStreak(emptySet(), today)) }
    @Test fun birthdayTodayIsNotSkipped() { assertEquals(LocalDate.parse("2026-09-11"), Schedule.nextBirthday(LocalDate.parse("2000-09-11"), LocalDate.parse("2026-09-11"))) }
    @Test fun emptyBirthdayOffsetsMeansNoReminder() { assertNull(Schedule.next(spec(kind = Kind.BIRTHDAY).copy(birthdayOffsets = emptySet()), Instant.now(), utc)) }
}
