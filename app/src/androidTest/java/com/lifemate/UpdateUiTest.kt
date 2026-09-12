package com.lifemate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.ui.*
import com.lifemate.updates.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdateUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun verifiedNewerVersionBlocksBackButOffersDownloadBackupAndExit() {
        var download = false; var backup = false; var closed = false
        val release = ReleaseInfo(BuildConfig.VERSION_CODE + 1,"test-newer","https://github.com/thisrony506-prog/LifeMate/releases")
        compose.setContent { LifeTheme("Light") { RequiredUpdateDialog(UpdateState(release),{ download=true },{ backup=true },{ closed=true }) } }
        compose.onNodeWithTag("required-update").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("required-update").assertIsDisplayed()
        compose.onNodeWithTag("required-download").performClick(); assertTrue(download)
        compose.onNodeWithText("Export a private backup").performScrollTo().performClick(); assertTrue(backup)
        compose.onNodeWithText("Close app").performClick(); assertTrue(closed)
        compose.onNodeWithTag("required-update").assertIsDisplayed()
    }
    @Test fun equalVersionNeverBlocksUse() {
        val release = ReleaseInfo(BuildConfig.VERSION_CODE,"current","https://github.com/thisrony506-prog/LifeMate/releases")
        compose.setContent { LifeTheme("Dark") { RequiredUpdateDialog(UpdateState(release),{},{},{}) } }
        compose.onNodeWithTag("required-update").assertDoesNotExist()
    }
    @Test fun offlineErrorWithoutVerifiedReleaseNeverBlocksUse() {
        compose.setContent { LifeTheme("Light") { RequiredUpdateDialog(UpdateState(message="offline"),{},{},{}) } }
        compose.onNodeWithTag("required-update").assertDoesNotExist()
    }
}
