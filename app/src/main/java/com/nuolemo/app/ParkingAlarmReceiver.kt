package com.nuolemo.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * 停车提醒接收器
 * 处理停车计时到期的提醒
 */
class ParkingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "停车提醒触发")

        val record = ParkingStore.getCurrentRecord(context)
        if (record == null) {
            Log.d(TAG, "没有当前停车记录，忽略提醒")
            return
        }

        val settings = ParkingStore.getSettings(context)
        val location = record.location ?: "车位未记录"
        val duration = record.getFormattedDuration()
        val body = "已停车 $duration\n$location"

        // 根据设置选择提醒方式
        if (settings.useFullScreenAlert) {
            // 复用挪车报警（AlarmService 全屏 + 响铃震动，失败时自动降级为紧急通知）
            Log.d(TAG, "使用全屏强提醒")
            AlarmLaunchHelper.startAlarm(context, "停车提醒", body)
        } else {
            // 使用普通高优先级通知
            Log.d(TAG, "使用高优先级通知")
            AlarmNotifier.showUrgentAlarmNotification(context, "停车提醒", body)
        }

        EventLogger.log(context, "停车提醒", "已停 $duration, 位置: $location")

        // 标记提醒已触发（避免重复提醒）
        val updatedRecord = record.copy(reminderScheduled = false)
        ParkingStore.saveCurrentRecord(context, updatedRecord)
    }

    companion object {
        private const val TAG = "ParkingAlarmReceiver"
        private const val REQUEST_CODE = 2001

        /**
         * 设置停车提醒
         */
        fun scheduleReminder(context: Context, reminderMinutes: Int) {
            if (reminderMinutes <= 0) {
                cancelReminder(context)
                return
            }

            val record = ParkingStore.getCurrentRecord(context) ?: return
            val triggerTime = record.startTime + reminderMinutes * 60 * 1000L

            val intent = Intent(context, ParkingAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "停车提醒已设置: ${reminderMinutes}分钟后")
                } else {
                    Log.w(TAG, "无精确闹钟权限，使用普通闹钟")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "停车提醒已设置: ${reminderMinutes}分钟后")
            }

            // 更新记录状态
            val updatedRecord = record.copy(
                reminderMinutes = reminderMinutes,
                reminderScheduled = true
            )
            ParkingStore.saveCurrentRecord(context, updatedRecord)
            EventLogger.log(context, "停车提醒", "设置 ${reminderMinutes}分钟后提醒")
        }

        /**
         * 取消停车提醒
         */
        fun cancelReminder(context: Context) {
            val intent = Intent(context, ParkingAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "停车提醒已取消")
        }
    }
}
