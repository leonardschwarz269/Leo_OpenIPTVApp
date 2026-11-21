package com.kraftplay.openiptv.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CacheMaintenanceTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `clear removes cache contents but keeps cache directory`() {
        val cache = temp.newFolder("cache")
        val a = java.io.File(cache, "a.bin").apply { writeText("12345") }
        val nested = java.io.File(cache, "images").apply { mkdirs() }
        java.io.File(nested, "b.png").writeText("123")
        val before = cacheSizeBytes(cache)
        assertEquals(8L, before)
        val freed = clearCacheFiles(cache)
        assertEquals(8L, freed)
        assertTrue(cache.exists())
        assertEquals(0L, cacheSizeBytes(cache))
        assertTrue(!a.exists())
    }

    @Test
    fun `missing cache directory is safe`() {
        val missing = java.io.File(temp.root, "missing")
        assertEquals(0L, cacheSizeBytes(missing))
        assertEquals(0L, clearCacheFiles(missing))
    }
}