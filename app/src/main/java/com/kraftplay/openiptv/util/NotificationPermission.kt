package com.kraftplay.openiptv.util

/** POST_NOTIFICATIONS нужно запрашивать runtime только на Android 13+ (API 33). */
fun shouldRequestNotificationPermission(sdkInt: Int, granted: Boolean): Boolean =
    sdkInt >= 33 && !granted