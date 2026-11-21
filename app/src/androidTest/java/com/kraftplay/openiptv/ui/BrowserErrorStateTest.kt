package com.kraftplay.openiptv.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserErrorStateTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun connectionRefusedShowsRetryInsteadOfWhiteScreen() {
        rule.setContent {
            BrowserScreen(initialUrl = "https://127.0.0.1:1/", onBack = {})
        }
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Повторить").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Повторить").assertIsDisplayed().performClick()
        // Closed loopback port must still surface an error after retry, not a blank WebView.
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Повторить").fetchSemanticsNodes().isNotEmpty()
        }
    }
}