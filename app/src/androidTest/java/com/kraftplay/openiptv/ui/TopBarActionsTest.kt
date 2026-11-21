package com.kraftplay.openiptv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kraftplay.openiptv.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopBarActionsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun render(isAuto: Boolean) {
        rule.setContent {
            MaterialTheme {
                TopBarActions(
                    isAuto = isAuto,
                    showAddPlaylist = true,
                    isEpgLoading = false,
                    categoryDisplayMode = "Chips",
                    onAddPlaylist = {},
                    onSearch = {},
                    onVoiceSearch = {},
                    onToggleDisplayMode = {},
                    onEpgSearch = {},
                    onRefresh = {},
                    onSettings = {},
                    onYouTube = {},
                    onRuTube = {},
                    onBrowser = {}
                )
            }
        }
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val localizedDescriptions = listOf(
        context.getString(R.string.new_playlist),
        context.getString(R.string.search),
        "Voice Search",
        "Toggle Display Mode",
        context.getString(R.string.epg_search),
        "Refresh",
        context.getString(R.string.settings)
    )

    @Test
    fun phoneActionsHaveAtLeast48dpTouchTargets() {
        render(isAuto = false)
        for (description in localizedDescriptions) {
            rule.onNodeWithContentDescription(description)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun autoActionsHaveAtLeast56dpTouchTargets() {
        render(isAuto = true)
        val descriptions = localizedDescriptions + listOf("YouTube", "RuTube", "Browser")
        for (description in descriptions) {
            rule.onNodeWithContentDescription(description)
                .assertWidthIsAtLeast(64.dp)
                .assertHeightIsAtLeast(64.dp)
        }
    }

    @Test
    fun settingsActionCanReceiveDpadFocus() {
        render(isAuto = false)
        rule.onNodeWithContentDescription(context.getString(R.string.settings)).requestFocus()
        rule.onNodeWithContentDescription(context.getString(R.string.settings)).assertIsFocused()
    }
}