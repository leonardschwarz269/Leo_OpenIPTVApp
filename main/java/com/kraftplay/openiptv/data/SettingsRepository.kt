package com.kraftplay.openiptv.data

import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(@PublishedApi internal val prefs: SharedPreferences) {

    @PublishedApi internal val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    inline fun <reified T> getObject(key: String, default: T): T {
        val raw = prefs.getString(key, null) ?: return default
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            default
        }
    }

    inline fun <reified T> putObject(key: String, value: T) {
        try {
            val encoded = json.encodeToString(value)
            prefs.edit().putString(key, encoded).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
