package com.lifemate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.lifemate.database.Profile
import kotlinx.coroutines.runBlocking
import org.junit.*

class LockNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as LifeMateApp
    @After fun cleanup() { app.secure.removePin() }
    @Test fun recreationRequiresPinBeforeAnyPrivateNavigationIsVisible() {
        runBlocking { app.db.dao().saveProfile(Profile(fullName = "Private profile", preferredName = "Private")) }
        app.secure.setPin("726391")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Your space, kept safe.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Search everything").assertDoesNotExist()
        compose.onNodeWithText("Your PIN").performTextInput("111111")
        compose.onNodeWithText("Unlock LifeMate").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("That PIN doesn't match. Try again.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Your PIN").performTextInput("726391")
        compose.onNodeWithText("Unlock LifeMate").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Search everything").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Your space, kept safe.").assertDoesNotExist()
    }
    @Test fun privateDialogIsDismissedAfterBackgroundTimeout() {
        runBlocking { app.db.dao().saveProfile(Profile(fullName = "Dialog privacy test")) }
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("App lock").performScrollTo().performClick()
        compose.onNodeWithText("New PIN").performTextInput("839271")
        compose.onNodeWithText("Confirm PIN").performTextInput("839271")
        compose.onNodeWithText("Save PIN").performClick()
        compose.waitUntil(10_000) { app.secure.hasPin() }
        compose.onNodeWithText("Change PIN").performScrollTo().performClick()
        compose.onNodeWithText("Current PIN").assertIsDisplayed()
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(31_000)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Your space, kept safe.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Current PIN").assertDoesNotExist()
        compose.onNodeWithText("Your PIN").performTextInput("839271")
        compose.onNodeWithText("Unlock LifeMate").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Make it feel like you").fetchSemanticsNodes().isNotEmpty() }
    }

}
