package com.kraftplay.openiptv

import org.junit.Assert.assertEquals
import org.junit.Test

class BackPoliciesTest {
    @Test
    fun `visible player controls consume back before exit`() {
        assertEquals(PlayerBackAction.HIDE_CONTROLS, playerBackAction(showInfoPanel = true))
    }

    @Test
    fun `player back exits when controls are already hidden`() {
        assertEquals(PlayerBackAction.EXIT, playerBackAction(showInfoPanel = false))
    }

    @Test
    fun `Exit setting exits immediately`() {
        val result = BackExitPolicy.onBack(behavior = "Exit", now = 1_000L)

        assertEquals(BackExitPolicy.Action.EXIT, result.action)
    }

    @Test
    fun `back presses less than 400ms apart are ignored`() {
        val first = BackExitPolicy.onBack("Double press to exit", 1_000L, BackExitPolicy.State())
        val spam = BackExitPolicy.onBack("Double press to exit", 1_399L, first.state)
        val stillSpam = BackExitPolicy.onBack("Double press to exit", 1_700L, spam.state)

        assertEquals(BackExitPolicy.Action.PROMPT, first.action)
        assertEquals(BackExitPolicy.Action.IGNORE, spam.action)
        assertEquals(BackExitPolicy.Action.IGNORE, stillSpam.action)
    }

    @Test
    fun `intentional second press from 400 through 1500ms exits`() {
        for (behavior in listOf("Confirm", "Double press to exit")) {
            for (delay in listOf(400L, 1_500L)) {
                val first = BackExitPolicy.onBack(behavior, 1_000L, BackExitPolicy.State())
                val second = BackExitPolicy.onBack(behavior, 1_000L + delay, first.state)

                assertEquals("$behavior after ${delay}ms", BackExitPolicy.Action.EXIT, second.action)
            }
        }
    }

    @Test
    fun `press after 1500ms starts a new exit window`() {
        val first = BackExitPolicy.onBack("Double press to exit", 1_000L, BackExitPolicy.State())
        val expired = BackExitPolicy.onBack("Double press to exit", 2_501L, first.state)
        val second = BackExitPolicy.onBack("Double press to exit", 2_901L, expired.state)

        assertEquals(BackExitPolicy.Action.PROMPT, expired.action)
        assertEquals(BackExitPolicy.Action.EXIT, second.action)
    }

    @Test
    fun `default back behavior is double press to exit`() {
        assertEquals("Double press to exit", BackExitPolicy.DEFAULT_BEHAVIOR)
    }

    @Test
    fun `back to list and unknown behaviors remain no-op`() {
        assertEquals(BackExitPolicy.Action.IGNORE, BackExitPolicy.onBack("Back to list", 1_000L).action)
        assertEquals(BackExitPolicy.Action.IGNORE, BackExitPolicy.onBack("Unknown behavior", 1_000L).action)
    }
}
