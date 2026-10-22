package com.nuolemo.app

import org.json.JSONObject

private fun formatParkingDuration(durationMs: Long, emptyText: String): String {
    val hours = durationMs / (1000 * 60 * 60)
    val minutes = (durationMs / (1000 * 60)) % 60
    return when {
        hours > 0 -> "${hours}小时${minutes}分钟"
        minutes > 0 -> "${minutes}分钟"
        else -> emptyText
    }
}

/**
 * 当前停车记录
 */
data class ParkingRecord(
    val startTime: Long,
    val location: String? = null,
    val photoPath: String? = null,
    val reminderMinutes: Int = 0,  // 0 表示不提醒
    val reminderScheduled: Boolean = false,
) {
    fun getFormattedDuration(): String =
        formatParkingDuration(System.currentTimeMillis() - startTime, "刚刚")

    fun toJson(): JSONObject = JSONObject()
        .put("startTime", startTime)
        .put("location", location ?: JSONObject.NULL)
        .put("photoPath", photoPath ?: JSONObject.NULL)
        .put("reminderMinutes", reminderMinutes)
        .put("reminderScheduled", reminderScheduled)

    companion object {
        fun fromJson(obj: JSONObject): ParkingRecord = ParkingRecord(
            startTime = obj.getLong("startTime"),
            location = obj.optString("location").takeIf { it.isNotEmpty() && !obj.isNull("location") },
            photoPath = obj.optString("photoPath").takeIf { it.isNotEmpty() && !obj.isNull("photoPath") },
            reminderMinutes = obj.optInt("reminderMinutes", 0),
            reminderScheduled = obj.optBoolean("reminderScheduled", false),
        )
    }
}

/**
 * 停车历史记录
 */
data class ParkingHistory(
    val startTime: Long,
    val endTime: Long,
    val location: String? = null,
    val photoPath: String? = null,
) {
    fun getFormattedDuration(): String =
        formatParkingDuration(endTime - startTime, "不足1分钟")

    fun toJson(): JSONObject = JSONObject()
        .put("startTime", startTime)
        .put("endTime", endTime)
        .put("location", location ?: JSONObject.NULL)
        .put("photoPath", photoPath ?: JSONObject.NULL)

    companion object {
        fun fromJson(obj: JSONObject): ParkingHistory = ParkingHistory(
            startTime = obj.getLong("startTime"),
            endTime = obj.getLong("endTime"),
            location = obj.optString("location").takeIf { it.isNotEmpty() && !obj.isNull("location") },
            photoPath = obj.optString("photoPath").takeIf { it.isNotEmpty() && !obj.isNull("photoPath") },
        )
    }
}
