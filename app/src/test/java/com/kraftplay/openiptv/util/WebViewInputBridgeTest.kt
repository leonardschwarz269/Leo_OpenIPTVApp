package com.kraftplay.openiptv.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewInputBridgeTest {

    @Test
    fun `sanitizeWebViewUserAgent strips wv and Version tokens while keeping Chrome version`() {
        val raw = "Mozilla/5.0 (Linux; Android 15; Pixel 6 Build/AP3A; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/131.0.6778.200 Mobile Safari/537.36"
        val sanitized = sanitizeWebViewUserAgent(raw)
        assertFalse(sanitized.contains("; wv"))
        assertFalse(sanitized.contains("Version/4.0"))
        assertTrue(sanitized.contains("Chrome/131.0.6778.200"))
        assertTrue(sanitized.contains("Pixel 6 Build/AP3A)"))
    }

    @Test
    fun `sanitizeWebViewUserAgent falls back when empty`() {
        val fallback = sanitizeWebViewUserAgent("   ")
        assertTrue(fallback.contains("Chrome/"))
        assertFalse(fallback.contains("; wv"))
    }

    @Test
    fun `shouldAutoShowCarKeyboard only auto-opens in car mode`() {
        assertFalse(shouldAutoShowCarKeyboard(isAuto = false))
        assertTrue(shouldAutoShowCarKeyboard(isAuto = true))
    }

    @Test
    fun `buildSetInputValueScript safely escapes quotes backslashes and newlines`() {
        val password = "P@ss'\"\\123\n\$_!"
        val script = buildSetInputValueScript(password, triggerChange = true)
        assertTrue(script.contains("P@ss\\'\\\"\\\\123\\n\$_!"))
        assertTrue(script.contains("HTMLInputElement"))
        assertTrue(script.contains("HTMLTextAreaElement"))
        assertTrue(script.contains("dispatchEvent"))
        assertTrue(script.contains("new Event('change'"))
        assertTrue(script.contains("__openIptvSettingValue"))
    }

    @Test
    fun `buildInstallAutoFocusListenerScript installs idempotent focusin listener with setting guard`() {
        val script = buildInstallAutoFocusListenerScript()
        assertTrue(script.contains("focusin"))
        assertTrue(script.contains("__openIptvFocusListenerInstalled"))
        assertTrue(script.contains("__openIptvSettingValue"))
        assertTrue(script.contains("CarBridge.onInputFocused"))
    }

    @Test
    fun `buildSetInputValueScript skips change event when triggerChange is false`() {
        val script = buildSetInputValueScript("user@example.com", triggerChange = false)
        assertTrue(script.contains("user@example.com"))
        assertFalse(script.contains("new Event('change'"))
    }

    @Test
    fun `classifyWebNavigation handles http intent fallback and custom schemes`() {
        assertEquals(WebNavAction.AllowInWebView, classifyWebNavigation("https://accounts.google.com/v3/signin"))
        assertEquals(WebNavAction.AllowInWebView, classifyWebNavigation("http://prisma.ws/"))

        val intentWithFallback = "intent://accounts.google.com#Intent;scheme=https;S.browser_fallback_url=https%3A%2F%2Faccounts.google.com%2Fsignin;end"
        assertEquals(
            WebNavAction.LoadFallbackUrl("https://accounts.google.com/signin"),
            classifyWebNavigation(intentWithFallback)
        )

        assertEquals(WebNavAction.BlockExternalScheme, classifyWebNavigation("vnd.youtube://watch?v=123"))
        assertEquals(WebNavAction.BlockExternalScheme, classifyWebNavigation("rutube://video/123"))
        assertEquals(WebNavAction.BlockExternalScheme, classifyWebNavigation("intent://open#Intent;package=com.example;end"))
        assertEquals(WebNavAction.BlockExternalScheme, classifyWebNavigation(null))
    }
}
