package com.lifemate.domain

import com.lifemate.database.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeeklyProgressTest {
    private val monday = LocalDate.of(2026,9,7)
    private fun mission() = LifeItem(id="mission",kind=Kind.MISSION,date=monday.toString(),duration=7)
    @Test fun emptyWeekHasNoInventedProgress() {
        val result = WeeklyProgress.calculate(emptyList(),emptyList(),monday)
        assertEquals(0,result.rate); assertEquals(0,result.scheduled); assertEquals(0f,result.fraction,0.001f)
    }
    @Test fun futureDaysAreNotMissedAndDuplicateCheckinsDoNotInflateTotals() {
        val done = listOf(Completion("mission",monday.toString()),Completion("mission",monday.toString()))
        val result = WeeklyProgress.calculate(listOf(mission()),done,monday.plusDays(2))
        assertEquals(7,result.scheduled); assertEquals(1,result.completed); assertEquals(3,result.due)
        assertEquals(1,result.missed); assertEquals(6,result.remaining); assertEquals(33,result.rate)
    }
    @Test fun archivesAndUnrelatedKindsExcluded() {
        val result = WeeklyProgress.calculate(listOf(mission().copy(archived=true),mission().copy(kind=Kind.NOTE),mission().copy(kind=Kind.BIRTHDAY)),emptyList(),monday)
        assertEquals(0,result.scheduled)
    }
    @Test fun futureCompletionCannotCountBeforeThatDay() {
        val result = WeeklyProgress.calculate(listOf(mission()),listOf(Completion("mission",monday.plusDays(2).toString())),monday)
        assertEquals(0,result.completed); assertEquals(0,result.missed)
    }
    @Test fun weekStartsMondayAcrossYearBoundary() {
        val day = LocalDate.of(2027,1,1)
        assertEquals(LocalDate.of(2026,12,28),WeeklyProgress.calculate(emptyList(),emptyList(),day).start)
    }
    @Test fun scheduledWeekdaysRespectCurrentSchedule() {
        val routine = mission().copy(kind=Kind.ROUTINE,repeat=Repeat.WEEKDAYS)
        val result = WeeklyProgress.calculate(listOf(routine),emptyList(),monday.plusDays(6))
        assertEquals(5,result.scheduled); assertEquals(5,result.missed)
    }
}
