package com.kraftplay.openiptv.service

import org.junit.Assert.assertEquals
import org.junit.Test

class DecoderModeTest {

    @Test
    fun `Auto maps to hardware with fallback`() =
        assertEquals(DecoderMode.Mode.HARDWARE_FALLBACK, DecoderMode.fromSetting("Auto"))

    @Test
    fun `HW maps to hardware with fallback`() =
        assertEquals(DecoderMode.Mode.HARDWARE_FALLBACK, DecoderMode.fromSetting("HW"))

    @Test
    fun `SW maps to software only`() =
        assertEquals(DecoderMode.Mode.SOFTWARE, DecoderMode.fromSetting("SW"))

    @Test
    fun `unknown value falls back to hardware`() =
        assertEquals(DecoderMode.Mode.HARDWARE_FALLBACK, DecoderMode.fromSetting("SomethingElse"))

    @Test
    fun `empty value falls back to hardware`() =
        assertEquals(DecoderMode.Mode.HARDWARE_FALLBACK, DecoderMode.fromSetting(""))
}