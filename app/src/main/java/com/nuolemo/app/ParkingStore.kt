package com.nuolemo.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 停车记录存储（SharedPreferences + org.json）
 */
object ParkingStore {
    private const val PREFS_NAME = "parking_prefs"
    private const val KEY_CURRENT_RECORD = "current_record"
    private const val KEY_HISTORY = "history"
    private const val KEY_SETTINGS = "settings"
    private const val MAX_HISTORY = 50

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCurrentRecord(context: Context, record: ParkingRecord?) {
        prefs(context).edit()
            .putString(KEY_CURRENT_RECORD, record?.toJson()?.toString())
            .apply()
    }

    fun getCurrentRecord(context: Context): ParkingRecord? {
        val json = prefs(context).getString(KEY_CURRENT_RECORD, null) ?: return null
        return try {
            ParkingRecord.fromJson(JSONObject(json))
        } catch (_: Exception) {
            null
        }
    }

    /** 结束停车：当前记录写入历史并清除 */
    fun endParking(context: Context) {
        val current = getCurrentRecord(context) ?: return
        addHistory(
            context,
            ParkingHistory(
                startTime = current.startTime,
                endTime = System.currentTimeMillis(),
                location = current.location,
                photoPath = current.photoPath,
            ),
        )
        saveCurrentRecord(context, null)
    }

    private fun addHistory(context: Context, history: ParkingHistory) {
        val list = (listOf(history) + getHistory(context)).take(MAX_HISTORY)
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs(context).edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun getHistory(context: Context): List<ParkingHistory> {
        val json = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { ParkingHistory.fromJson(array.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context) {
        prefs(context).edit().remove(KEY_HISTORY).apply()
    }

    fun saveSettings(context: Context, settings: ParkingSettings) {
        prefs(context).edit().putString(KEY_SETTINGS, settings.toJson().toString()).apply()
    }

    fun getSettings(context: Context): ParkingSettings {
        val json = prefs(context).getString(KEY_SETTINGS, null) ?: return ParkingSettings()
        return try {
            ParkingSettings.fromJson(JSONObject(json))
        } catch (_: Exception) {
            ParkingSettings()
        }
    }
}
