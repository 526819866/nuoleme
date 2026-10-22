package com.nuolemo.app

import org.json.JSONObject

/**
 * 停车提醒设置（AlarmMode 复用挪车报警的枚举）
 */
data class ParkingSettings(
    val reminderAlarmMode: AlarmMode = AlarmMode.SOUND_AND_VIBRATE,
    val useFullScreenAlert: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("reminderAlarmMode", reminderAlarmMode.name)
        .put("useFullScreenAlert", useFullScreenAlert)

    companion object {
        fun fromJson(obj: JSONObject): ParkingSettings = ParkingSettings(
            reminderAlarmMode = runCatching {
                AlarmMode.valueOf(obj.optString("reminderAlarmMode"))
            }.getOrDefault(AlarmMode.SOUND_AND_VIBRATE),
            useFullScreenAlert = obj.optBoolean("useFullScreenAlert", true),
        )
    }
}
