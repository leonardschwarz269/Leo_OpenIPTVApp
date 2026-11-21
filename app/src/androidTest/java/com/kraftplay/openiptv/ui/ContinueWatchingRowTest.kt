package com.kraftplay.openiptv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kraftplay.openiptv.R
import com.kraftplay.openiptv.model.IptvChannel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContinueWatchingRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun watchedChannelIsShownAndClickable() {
        val channel = IptvChannel(name = "Test Channel", url = "http://example.com/stream")
        var clicked: IptvChannel? = null
        rule.setContent {
            MaterialTheme {
                ContinueWatchingRow(
                    channels = listOf(channel),
                    viewingTimes = mapOf(channel.url to 120L),
                    currentPrograms = emptyMap(),
                    nextPrograms = emptyMap(),
                    fontSize = 14f,
                    onChannelClick = { clicked = it },
                    onLongClick = {}
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.continue_watching)).assertIsDisplayed()
        rule.onNodeWithText("Test Channel").assertIsDisplayed().performClick()
        assertEquals(channel, clicked)
    }

    @Test
    fun unwatchedHistoryRendersNothing() {
        val channel = IptvChannel(name = "Test Channel", url = "http://example.com/stream")
        rule.setContent {
            MaterialTheme {
                ContinueWatchingRow(
                    channels = listOf(channel),
                    viewingTimes = emptyMap(),
                    currentPrograms = emptyMap(),
                    nextPrograms = emptyMap(),
                    fontSize = 14f,
                    onChannelClick = {},
                    onLongClick = {}
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.continue_watching)).assertDoesNotExist()
    }
}