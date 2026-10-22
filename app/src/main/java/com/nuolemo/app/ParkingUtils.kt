package com.nuolemo.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 停车计时的工具类
 */
object ParkingUtils {
    /**
     * 格式化停车时长
     */
    fun formatDuration(durationMs: Long): String {
        val hours = (durationMs / (1000 * 60 * 60)).toInt()
        val minutes = ((durationMs / (1000 * 60)) % 60).toInt()
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "刚刚"
        }
    }

    /**
     * 格式化时间戳为可读时间
     */
    fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * 格式化日期时间
     */
    fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * 获取提醒时间文本
     */
    fun getReminderText(context: Context, reminderMinutes: Int): String {
        return when (reminderMinutes) {
            0 -> context.getString(R.string.parking_reminder_option_none)
            30 -> context.getString(R.string.parking_reminder_option_30)
            60 -> context.getString(R.string.parking_reminder_option_60)
            90 -> context.getString(R.string.parking_reminder_option_90)
            120 -> context.getString(R.string.parking_reminder_option_120)
            180 -> context.getString(R.string.parking_reminder_option_180)
            else -> "${reminderMinutes}分钟"
        }
    }

    /**
     * 获取报警模式文本
     */
    fun getAlarmModeText(context: Context, mode: ParkingAlarmMode): String {
        return when (mode) {
            ParkingAlarmMode.INHERIT -> context.getString(R.string.parking_alarm_mode_inherit)
            ParkingAlarmMode.SOUND_AND_VIBRATE -> context.getString(R.string.parking_alarm_mode_sound_vibrate)
            ParkingAlarmMode.SOUND_ONLY -> context.getString(R.string.parking_alarm_mode_sound)
            ParkingAlarmMode.VIBRATE_ONLY -> context.getString(R.string.parking_alarm_mode_vibrate)
            ParkingAlarmMode.NOTIFICATION_ONLY -> context.getString(R.string.parking_alarm_mode_notification)
        }
    }
}
