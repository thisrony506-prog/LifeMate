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
    @Test fun updatesLiveInSettingsAndExplainTheRequiredPolicy() {
        compose.onNodeWithTag("open-updates").assertDoesNotExist()
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithTag("drawer-settings").performScrollTo().performClick()
        compose.onNodeWithText("App updates").performScrollTo().performClick()
        compose.onNodeWithText("Keep LifeMate up to date").assertIsDisplayed()
        compose.onNodeWithText("Automatic update checks").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("A verified newer version requires installation", substring = true).assertExists()
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
        compose.onNodeWithTag("page-title").assertTextEquals("Missions")
        compose.onNodeWithTag("nav-Calendar").performClick()
        compose.onNodeWithTag("page-title").assertTextEquals("Calendar")
        compose.onNodeWithTag("nav-Memories").performClick()
        compose.onNodeWithTag("page-title").assertTextEquals("Memories")
        compose.onNodeWithTag("nav-Profile").performClick()
        compose.onNodeWithText("Rony Test").assertIsDisplayed()
        compose.onNodeWithText("Settings & privacy", substring = true).performScrollTo().performClick()
        compose.onNodeWithTag("appearance-settings").performScrollTo()
        compose.onNodeWithText("Dark").performClick()
        compose.waitUntil(10_000) { runBlocking { app.preferences.flow.first().theme == "Dark" } }
        compose.onNodeWithTag("appearance-settings").performScrollTo()
        compose.onNodeWithText("Light").performClick()
    }
    @Test fun brandedHeaderProfileShortcutAndEveryFeatureCardWork() {
        compose.onNodeWithContentDescription("LifeMate logo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open your profile").performClick()
        compose.onNodeWithText("Rony Test").assertIsDisplayed()
        compose.onNodeWithContentDescription("All features").performClick()
        Kind.entries.forEach { kind ->
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("feature-${kind.name}"))
            compose.onNodeWithTag("feature-${kind.name}").assertIsDisplayed().performClick()
            compose.onNodeWithTag("page-title").assertTextEquals(kind.plural)
            compose.runOnUiThread {compose.activity.onBackPressedDispatcher.onBackPressed()}
            compose.onNodeWithContentDescription("LifeMate logo").assertIsDisplayed()
        }
    }
    @Test fun photoStudioEditsAndRestoresAnEncryptedDraft() {
        compose.onNodeWithTag("nav-Profile").performClick()
        compose.onNodeWithContentDescription("All features").performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Photo & card studio", substring = true))
        compose.onNodeWithText("Photo & card studio", substring = true).performClick()
        compose.onNodeWithText("Your photo studio").assertIsDisplayed()
        compose.onNodeWithText("Your words (up to 600 characters)").performScrollTo().performTextReplacement("A bright new chapter")
        compose.waitUntil(15_000) { app.secure.readStudioDraft("social-post")?.contains("A bright new chapter") == true }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Your words (up to 600 characters)").performScrollTo().assertTextContains("A bright new chapter")
    }
    @Test fun facebookPostComposerSavesAndReopensLocalCaption() {
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithTag("drawer-posts").performScrollTo().performClick()
        compose.onAllNodesWithText("Create post").onFirst().performClick()
        compose.onNodeWithText("Post topic").performTextInput("Reading milestone")
        compose.onNodeWithText("Short message").performTextInput("30 days completed")
        compose.onNodeWithText("Create caption").performScrollTo().performClick()
        compose.onNodeWithTag("save-post").performScrollTo().performClick()
        compose.waitUntil(15_000) {runBlocking {app.db.dao().allPosts().any {it.topic=="Reading milestone" && it.caption.contains("30 days completed")}}}
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertEquals(1,runBlocking {app.db.dao().allPosts().count {it.topic=="Reading milestone"}})
    }
    @Test fun missionCheckinIsPersistedAndUnique() {
        val item = LifeItem(kind = Kind.MISSION, title = "Reading journey", date = LocalDate.now().toString(), duration = 7, notifications = false)
        runBlocking { app.db.dao().save(item) }
        compose.waitForIdle()
        // Put the single task at the top, clear of the bottom-right floating add button.
        // The feed has five introductory rows before its first scheduled task.
        compose.onNodeWithTag("home-feed").performScrollToIndex(5)
        compose.onNodeWithContentDescription("Complete Reading journey").performClick()
        compose.waitUntil(10_000) { runBlocking { app.db.dao().isDone(item.id, LocalDate.now().toString()) } }
        compose.onNodeWithContentDescription("Mark Reading journey incomplete").performClick()
        compose.waitUntil(10_000) { runBlocking { !app.db.dao().isDone(item.id, LocalDate.now().toString()) } }
    }
}
