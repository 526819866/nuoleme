package com.nuolemo.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 停车记录存储
 */
object ParkingStore {
    private const val PREFS_NAME = "parking_prefs"
    private const val KEY_CURRENT_RECORD = "current_record"
    private const val KEY_HISTORY = "history"
    private const val KEY_SETTINGS = "settings"
    private const val MAX_HISTORY = 50  // 最多保留50条历史记录

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存当前停车记录
     */
    fun saveCurrentRecord(context: Context, record: ParkingRecord?) {
        val json = if (record != null) gson.toJson(record) else null
        getPrefs(context).edit()
            .putString(KEY_CURRENT_RECORD, json)
            .apply()
    }

    /**
     * 获取当前停车记录
     */
    fun getCurrentRecord(context: Context): ParkingRecord? {
        val json = getPrefs(context).getString(KEY_CURRENT_RECORD, null) ?: return null
        return try {
            gson.fromJson(json, ParkingRecord::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 结束停车，将当前记录移到历史
     */
    fun endParking(context: Context) {
        val current = getCurrentRecord(context) ?: return
        val history = ParkingHistory(
            startTime = current.startTime,
            endTime = System.currentTimeMillis(),
            location = current.location,
            photoPath = current.photoPath
        )
        addHistory(context, history)
        saveCurrentRecord(context, null)
    }

    /**
     * 添加历史记录
     */
    private fun addHistory(context: Context, history: ParkingHistory) {
        val list = getHistory(context).toMutableList()
        list.add(0, history)  // 最新的在前面

        // 限制历史记录数量
        if (list.size > MAX_HISTORY) {
            list.subList(MAX_HISTORY, list.size).clear()
        }

        val json = gson.toJson(list)
        getPrefs(context).edit()
            .putString(KEY_HISTORY, json)
            .apply()
    }

    /**
     * 获取停车历史列表
     */
    fun getHistory(context: Context): List<ParkingHistory> {
        val json = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ParkingHistory>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 清空历史记录
     */
    fun clearHistory(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_HISTORY)
            .apply()
    }

    /**
     * 保存停车提醒设置
     */
    fun saveSettings(context: Context, settings: ParkingSettings) {
        val json = gson.toJson(settings)
        getPrefs(context).edit()
            .putString(KEY_SETTINGS, json)
            .apply()
    }

    /**
     * 获取停车提醒设置
     */
    fun getSettings(context: Context): ParkingSettings {
        val json = getPrefs(context).getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ParkingSettings::class.java)
            } catch (e: Exception) {
                ParkingSettings()
            }
        } else {
            ParkingSettings()
        }
    }

    fun getHistory(context: Context): List<ParkingHistory> {
        val json = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                ParkingHistory(
                    startTime = obj.getLong("startTime"),
                    endTime = obj.getLong("endTime"),
                    duration = obj.getLong("duration"),
                    location = obj.optString("location").takeIf { it.isNotEmpty() },
                    photoPath = obj.optString("photoPath").takeIf { it.isNotEmpty() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context) {
        prefs(context).edit().remove(KEY_HISTORY).apply()
    }
}

/**
 * 停车记录的便捷方法
 */
fun ParkingRecord.getFormattedDuration(): String {
    val now = System.currentTimeMillis()
    val durationMs = now - startTime
    val hours = (durationMs / (1000 * 60 * 60)).toInt()
    val minutes = ((durationMs / (1000 * 60)) % 60).toInt()
    return when {
        hours > 0 -> "${hours}小时${minutes}分钟"
        minutes > 0 -> "${minutes}分钟"
        else -> "刚刚"
    }
}

/**
 * 停车历史的便捷方法
 */
fun ParkingHistory.getFormattedDuration(): String {
    val hours = (duration / (1000 * 60 * 60)).toInt()
    val minutes = ((duration / (1000 * 60)) % 60).toInt()
    return when {
        hours > 0 -> "${hours}小时${minutes}分钟"
        minutes > 0 -> "${minutes}分钟"
        else -> "不到1分钟"
    }
}
