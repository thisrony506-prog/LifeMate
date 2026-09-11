package com.lifemate.domain

import com.lifemate.database.*
import com.lifemate.ui.LifeState
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    @Test fun archivingDoesNotEraseMissionProgress() {
        val item = LifeItem(kind = Kind.MISSION, title = "Read", date = "2026-09-01", duration = 3, archived = true)
        val state = LifeState(completions = listOf(Completion(item.id, "2026-09-01"), Completion(item.id, "2026-09-02")))
        assertEquals(2f / 3, state.progress(item), .0001f)
    }
    @Test fun oneTimeCompletionIsNotResetTheNextDay() {
        val item = LifeItem(title = "Appointment", date = "2026-09-01", repeat = Repeat.ONCE)
        val state = LifeState(completions = listOf(Completion(item.id, "2026-09-01")))
        assertTrue(state.completed(item, LocalDate.parse("2026-09-11")))
    }
    @Test fun recurringCompletionIsDateSpecific() {
        val item = LifeItem(title = "Exercise", date = "2026-09-01", repeat = Repeat.DAILY)
        val state = LifeState(completions = listOf(Completion(item.id, "2026-09-01")))
        assertFalse(state.completed(item, LocalDate.parse("2026-09-02")))
    }
}
