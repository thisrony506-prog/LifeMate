package com.lifemate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.lifemate.database.*
import com.lifemate.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

/** Uses real Compose navigation and the real encrypted on-device Room database. */
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as LifeMateApp
    @Before fun seedProfile() {
        runBlocking {
            app.db.dao().clear()
            app.db.dao().saveProfile(Profile(fullName = "Rony Test", preferredName = "Rony"))
            app.preferences.set("theme", "Light")
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Search everything").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun noteCreationPersistsAcrossActivityRecreation() {
        compose.onNodeWithContentDescription("Create something new").performClick()
        compose.onAllNodes(hasScrollToNodeAction()).onLast().performScrollToNode(hasText("New note"))
        compose.onNodeWithText("New note").performClick()
        compose.onNodeWithText("Note name *").performTextInput("A little clarity")
        compose.onNodeWithText("Your note").performTextInput("A real offline note")
        compose.onNodeWithText("Save note", substring = true).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("A little clarity").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("A real offline note", runBlocking { app.db.dao().allItems().first { it.title == "A little clarity" }.description })
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertTrue(runBlocking { app.db.dao().allItems().any { it.title == "A little clarity" } })
    }
    @Test fun bottomNavigationAndThemeAreReal() {
        compose.onNodeWithTag("nav-Missions").performClick()
        compose.onNode(hasText("Big changes begin with small commitments.") and hasTestTag("page-subtitle")).assertIsDisplayed()
        compose.onNodeWithTag("nav-Calendar").performClick()
        compose.onNodeWithText("A little perspective").assertIsDisplayed()
        compose.onNodeWithTag("nav-Memories").performClick()
        compose.onNode(hasText("Keep the moments. Remember the feeling.") and hasTestTag("page-subtitle")).assertIsDisplayed()
        compose.onNodeWithTag("nav-Profile").performClick()
        compose.onNodeWithText("Rony Test").assertIsDisplayed()
        compose.onNodeWithText("Settings & privacy", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { app.preferences.flow.first().theme == "Dark" } }
        compose.onNodeWithText("Light").performScrollTo().performClick()
    }
    @Test fun missionCheckinIsPersistedAndUnique() {
        val item = LifeItem(kind = Kind.MISSION, title = "Reading journey", date = LocalDate.now().toString(), duration = 7, notifications = false)
        runBlocking { app.db.dao().save(item) }
        compose.waitForIdle()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Reading journey"))
        compose.onNodeWithContentDescription("Complete Reading journey").performClick()
        compose.waitUntil(10_000) { runBlocking { app.db.dao().isDone(item.id, LocalDate.now().toString()) } }
        compose.onNodeWithContentDescription("Mark Reading journey incomplete").performClick()
        compose.waitUntil(10_000) { runBlocking { !app.db.dao().isDone(item.id, LocalDate.now().toString()) } }
    }
}
