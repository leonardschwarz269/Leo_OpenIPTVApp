package com.kraftplay.openiptv.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionTest {

    @Test
    fun `requests on API 33 when not granted`() =
        assertTrue(shouldRequestNotificationPermission(sdkInt = 33, granted = false))

    @Test
    fun `requests on API 35 when not granted`() =
        assertTrue(shouldRequestNotificationPermission(sdkInt = 35, granted = false))

    @Test
    fun `no request when already granted`() =
        assertFalse(shouldRequestNotificationPermission(sdkInt = 33, granted = true))

    @Test
    fun `no request below API 33`() {
        assertFalse(shouldRequestNotificationPermission(sdkInt = 32, granted = false))
        assertFalse(shouldRequestNotificationPermission(sdkInt = 24, granted = false))
    }
}