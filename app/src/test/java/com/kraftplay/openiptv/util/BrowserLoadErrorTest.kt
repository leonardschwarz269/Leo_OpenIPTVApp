package com.kraftplay.openiptv.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserLoadErrorTest {
    @Test
    fun `ssl error explains certificate problem`() {
        val message = browserLoadErrorMessage(isSsl = true, description = null)
        assertTrue(message.contains("сертификат", ignoreCase = true))
    }

    @Test
    fun `generic error keeps useful description`() {
        val message = browserLoadErrorMessage(isSsl = false, description = "net::ERR_CONNECTION_REFUSED")
        assertTrue(message.contains("ERR_CONNECTION_REFUSED"))
    }

    @Test
    fun `generic error without description is not blank`() {
        assertTrue(browserLoadErrorMessage(isSsl = false, description = null).isNotBlank())
    }

    @Test
    fun `ssl error for main frame is shown`() {
        assertTrue(shouldShowMainFrameSslError("https://example.com/", "https://example.com/"))
    }

    @Test
    fun `ssl error for subresource does not replace whole page`() {
        assertFalse(shouldShowMainFrameSslError("https://example.com/", "https://bad.example.com/frame"))
        assertFalse(shouldShowMainFrameSslError("https://example.com/", null))
    }
}