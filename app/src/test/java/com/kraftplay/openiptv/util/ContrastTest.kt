package com.kraftplay.openiptv.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    @Test
    fun `black and white have maximum contrast`() {
        assertEquals(21.0, contrastRatio(0xFFFFFF, 0x000000), 0.01)
    }

    @Test
    fun `dark scrim over white keeps white text readable`() {
        val scrim = compositeOver(0x000000, 0xFFFFFF, 0.85f)
        assertTrue(contrastRatio(0xFFFFFF, scrim) >= 4.5)
    }

    @Test
    fun `light glass over white fails normal text contrast`() {
        val glass = compositeOver(0xFFFFFF, 0xFFFFFF, 0.12f)
        assertTrue(contrastRatio(0xFFFFFF, glass) < 4.5)
    }
}