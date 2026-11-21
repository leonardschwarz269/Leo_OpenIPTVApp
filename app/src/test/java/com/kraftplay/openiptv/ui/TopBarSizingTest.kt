package com.kraftplay.openiptv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TopBarSizingTest {
    @Test
    fun `phone touch target is at least Material minimum`() {
        assertEquals(48f, topBarTargetSize(isAuto = false).value, 0f)
    }

    @Test
    fun `phone icon is enlarged to 24dp`() {
        assertEquals(24f, topBarIconSize(isAuto = false).value, 0f)
    }

    @Test
    fun `auto touch target is larger than phone`() {
        assertEquals(64f, topBarTargetSize(isAuto = true).value, 0f)
    }

    @Test
    fun `auto icon is enlarged to 28dp`() {
        assertEquals(32f, topBarIconSize(isAuto = true).value, 0f)
    }
}