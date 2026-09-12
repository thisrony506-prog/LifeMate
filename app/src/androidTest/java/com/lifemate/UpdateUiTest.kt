package com.lifemate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lifemate.ui.LifeTheme
import com.lifemate.ui.UpdateBanner
import com.lifemate.updates.ReleaseInfo
import com.lifemate.updates.UpdateState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdateUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun newerReleaseHasWorkingTopBanner() {
        var opened = false
        val release = ReleaseInfo(BuildConfig.VERSION_CODE + 1, "test-newer", "https://github.com/thisrony506-prog/LifeMate/releases")
        compose.setContent { LifeTheme("Light") { UpdateBanner(UpdateState(release)) { opened = true } } }
        compose.onNodeWithTag("update-banner").assertIsDisplayed()
        compose.onNodeWithText("View update").performClick()
        assertTrue(opened)
    }
    @Test fun equalReleaseDoesNotOfferAnUpdate() {
        val release = ReleaseInfo(BuildConfig.VERSION_CODE, "test-current", "https://github.com/thisrony506-prog/LifeMate/releases")
        compose.setContent { LifeTheme("Dark") { UpdateBanner(UpdateState(release)) {} } }
        compose.onNodeWithTag("update-banner").assertDoesNotExist()
        compose.onNodeWithTag("open-updates").assertIsDisplayed()
    }
}
