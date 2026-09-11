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
}
