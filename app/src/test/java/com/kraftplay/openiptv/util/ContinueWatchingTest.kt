package com.kraftplay.openiptv.util

import com.kraftplay.openiptv.model.IptvChannel
import org.junit.Assert.assertEquals
import org.junit.Test

class ContinueWatchingTest {
    private fun channel(url: String) = IptvChannel(name = url, url = url)

    @Test
    fun `only channels with positive viewing time are returned`() {
        val recent = listOf(channel("a"), channel("b"), channel("c"))
        val times = mapOf("a" to 60L, "b" to 0L)
        assertEquals(listOf("a"), continueWatchingChannels(recent, times).map { it.url })
    }

    @Test
    fun `recent order is preserved and limit applied`() {
        val recent = (1..8).map { channel("ch$it") }
        val times = recent.associate { it.url to 10L }
        assertEquals(listOf("ch1", "ch2", "ch3", "ch4", "ch5"), continueWatchingChannels(recent, times).map { it.url })
    }

    @Test
    fun `empty history yields empty row`() {
        assertEquals(emptyList<IptvChannel>(), continueWatchingChannels(listOf(channel("a")), emptyMap()))
    }
}