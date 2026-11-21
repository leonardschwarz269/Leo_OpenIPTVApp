package com.kraftplay.openiptv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenPresetsTest {
    @Test
    fun `car preset meets parked touch and text minimums`() {
        assertEquals(64f, ScreenPresets.targetSize(isAuto = true), 0f)
        assertEquals(32f, ScreenPresets.iconSize(isAuto = true), 0f)
        assertEquals(24f, ScreenPresets.gap(isAuto = true), 0f)
        assertEquals(24f, ScreenPresets.minTextSp(isAuto = true), 0f)
    }

    @Test
    fun `phone preset keeps Material minimum`() {
        assertEquals(48f, ScreenPresets.targetSize(isAuto = false), 0f)
        assertEquals(24f, ScreenPresets.iconSize(isAuto = false), 0f)
    }
}