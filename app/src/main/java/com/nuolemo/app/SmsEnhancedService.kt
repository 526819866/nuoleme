package com.nuolemo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Telephony
import android.util.Log
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 增强模式服务 - 用于小米等不发送短信广播的系统
 *
 * 通过 ContentObserver 监听短信数据库变化，确保能收到所有短信。
 * 只在用户开启"增强模式"时运行。
 */
class SmsEnhancedService : Service() {

    private var smsObserver: SmsObserver? = null
    private var startTime: Long = 0
    private var smsProcessedCount: Int = 0
    private var lastCheckTime: Long = 0
    private var shizukuAvailable: Boolean = false

    private val shizukuBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        shizukuAvailable = Shizuku.pingBinder()
        Log.d(TAG, "Shizuku 连接状态: $shizukuAvailable")
        updateNotification()
    }

    private val shizukuBinderDeadListener = Shizuku.OnBinderDeadListener {
        shizukuAvailable = false
        Log.d(TAG, "Shizuku 连接断开")
        updateNotification()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "增强模式服务启动")
        startTime = System.currentTimeMillis()

        // 初始化 Shizuku
        initShizuku()

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

    private fun initShizuku() {
        try {
            // 检查 Shizuku 是否可用
            shizukuAvailable = Shizuku.pingBinder()

            if (shizukuAvailable) {
                // 注册 Shizuku 监听器
                Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceivedListener)
                Shizuku.addBinderDeadListener(shizukuBinderDeadListener)

                Log.d(TAG, "Shizuku 已连接，增强保活功能已启用")
                EventLogger.log(this, "增强模式", "Shizuku 保活已启用")
            } else {
                Log.d(TAG, "Shizuku 未运行，使用标准前台服务")
                EventLogger.log(this, "增强模式", "Shizuku 未运行，使用标准保活")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku 初始化失败: ${e.message}")
            shizukuAvailable = false
        }
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
        lastCheckTime = System.currentTimeMillis()
        smsProcessedCount++
        updateNotification()

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

        val runningTime = formatRunningTime(System.currentTimeMillis() - startTime)
        val lastCheckText = if (lastCheckTime > 0) {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            "最近检查: ${formatter.format(Date(lastCheckTime))}"
        } else {
            "等待短信中"
        }

        val statusText = buildString {
            append("监听中 · ")
            if (shizukuAvailable) {
                append("Shizuku 保活 · ")
            }
            append("已运行 $runningTime")
        }

        val detailText = "已处理 $smsProcessedCount 条短信 · $lastCheckText"

        return Notification.Builder(this, channelId)
            .setContentTitle(statusText)
            .setContentText(detailText)
            .setSmallIcon(R.drawable.ic_car)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatRunningTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}天${hours % 24}小时"
            hours > 0 -> "${hours}小时${minutes % 60}分"
            minutes > 0 -> "${minutes}分钟"
            else -> "${seconds}秒"
        }
    }

    override fun onDestroy() {
        // 移除 Shizuku 监听器
        try {
            if (shizukuAvailable) {
                Shizuku.removeBinderReceivedListener(shizukuBinderReceivedListener)
                Shizuku.removeBinderDeadListener(shizukuBinderDeadListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku 监听器移除失败: ${e.message}")
        }

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
