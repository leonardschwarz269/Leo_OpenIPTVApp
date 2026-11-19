package com.kraftplay.openiptv.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {

    private fun parseExtinf(extinf: String): String =
        M3uParser.parse(("#EXTM3U\n" + extinf + "\nhttp://example.com/1.m3u8\n").byteInputStream()).single().name

    @Test
    fun `comma in channel name is preserved`() =
        assertEquals("Матч, Футбол", parseExtinf("#EXTINF:-1,Матч, Футбол"))

    @Test
    fun `comma inside quoted attribute does not split name`() =
        assertEquals("Первый канал", parseExtinf("#EXTINF:-1 tvg-name=\"a,b\" tvg-id=\"x\",Первый канал"))

    @Test
    fun `plain name still works`() =
        assertEquals("НТВ", parseExtinf("#EXTINF:-1 tvg-logo=\"http://l.png\",НТВ"))

    @Test
    fun `name with comma and attributes`() =
        assertEquals("Матч, Футбол", parseExtinf("#EXTINF:-1 tvg-id=\"match\" group-title=\"Спорт\",Матч, Футбол"))
}
