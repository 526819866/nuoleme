package com.nuolemo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log

/**
 * 增强模式服务 - 用于小米等不发送短信广播的系统
 *
 * 通过 ContentObserver 监听短信数据库变化，确保能收到所有短信。
 * 只在用户开启"增强模式"时运行。
 */
class SmsEnhancedService : Service() {

    private var smsObserver: SmsObserver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "增强模式服务启动")

        // 注册短信数据库监听
        smsObserver = SmsObserver(this) { body, timestamp ->
            handleSmsFromDatabase(body, timestamp)
        }

        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver!!
        )

        EventLogger.log(this, "增强模式", "ContentObserver 已注册")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = SettingsStore.load(this)

        // 检查是否开启增强模式
        if (!settings.enabled || !settings.enhancedMode) {
            Log.d(TAG, "增强模式已关闭，停止服务")
            EventLogger.log(this, "增强模式", "用户已关闭，停止服务")
            stopSelf()
            return START_NOT_STICKY
        }

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        EventLogger.log(this, "增强模式", "前台服务已启动")

        return START_STICKY
    }

    private fun handleSmsFromDatabase(body: String, timestamp: Long) {
        val settings = SettingsStore.load(this)

        if (!settings.enabled || !settings.enhancedMode) {
            return
        }

        // 从短信内容中提取发送者（如果有）
        val sender = extractSender(body)

        val matchResult = SmsMatcher.matchWithReason(settings, sender, body)

        val triggered = if (matchResult.matched) {
            Log.d(TAG, "匹配成功: ${matchResult.reason}")
            AlarmLaunchHelper.startAlarm(this, sender, body)
            true
        } else {
            Log.d(TAG, "未匹配: ${matchResult.reason}")
            false
        }

        EventLogger.logSmsReceived(this, sender, body, matchResult, triggered)
        EventLogger.log(this, "增强模式处理", "来源: 数据库, 触发: $triggered")
    }

    private fun extractSender(body: String): String? {
        // 尝试从短信内容中提取发送者（如【广州交警】）
        val pattern = Regex("^【(.+?)】")
        val match = pattern.find(body)
        return match?.groupValues?.get(1)
    }

    private fun createNotification(): Notification {
        val channelId = "sms_enhanced_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "增强短信监听",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "确保小米等系统能收到所有短信"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, channelId)
            .setContentTitle("挪了么 - 增强模式")
            .setContentText("正在监听短信（适用于小米等系统）")
            .setSmallIcon(R.drawable.ic_car)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        smsObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        Log.d(TAG, "增强模式服务停止")
        EventLogger.log(this, "增强模式", "ContentObserver 已注销，服务停止")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SmsEnhancedService"
        private const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, SmsEnhancedService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SmsEnhancedService::class.java)
            context.stopService(intent)
        }
    }
}
