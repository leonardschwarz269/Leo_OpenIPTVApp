package com.kraftplay.openiptv.service

/** Режим декодера плеера по настройке "hardware_decoder" ("Auto"/"HW"/"SW"). */
object DecoderMode {
    enum class Mode { HARDWARE_FALLBACK, SOFTWARE }

    fun fromSetting(setting: String): Mode = when (setting) {
        "SW" -> Mode.SOFTWARE
        else -> Mode.HARDWARE_FALLBACK // Auto/HW/unknown — аппаратный с программным fallback
    }
}