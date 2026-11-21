package com.kraftplay.openiptv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryChipSizingTest {
    @Test
    fun `phone category label is at least 16sp`() {
        assertEquals(16f, categoryChipLabelFontSize(14f, isAuto = false), 0f)
        assertEquals(20f, categoryChipLabelFontSize(22f, isAuto = false), 0f)
    }

    @Test
    fun `auto category label is at least 24sp`() {
        assertEquals(24f, categoryChipLabelFontSize(14f, isAuto = true), 0f)
        assertEquals(30f, categoryChipLabelFontSize(32f, isAuto = true), 0f)
    }
}