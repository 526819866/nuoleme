package com.nuolemo.app

/**
 * 停车提醒设置
 */
data class ParkingSettings(
    val reminderAlarmMode: AlarmMode = AlarmMode.SOUND_AND_VIBRATE,
    val reminderDurationSeconds: Int = 60,  // 提醒持续时长
    val useFullScreenAlert: Boolean = true   // 是否使用全屏提醒（类似挪车报警）
)
