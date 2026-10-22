package com.nuolemo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 停车计时前台服务
 * 用于在通知栏显示实时停车计时（支持动态岛/超级岛）
 */
class ParkingTimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var updateTask: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        startTimerUpdate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ParkingStore.endParking(this)
                stopSelf()
            }
            ACTION_UPDATE -> {
                updateNotification()
            }
        }
        return START_STICKY
    }

    private fun startTimerUpdate() {
        updateTask = object : Runnable {
            override fun run() {
                updateNotification()
                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        }
        handler.post(updateTask!!)
    }

    private fun createNotification(): Notification {
        val channelId = "parking_timer_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "停车计时",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示当前停车时长"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val record = ParkingStore.getCurrentRecord(this)
        if (record == null) {
            stopSelf()
            return createEmptyNotification(channelId)
        }

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ParkingTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 自定义通知布局（用于更好的动态岛支持）
        val customView = RemoteViews(packageName, R.layout.notification_parking_timer).apply {
            setTextViewText(R.id.textParkingDuration, record.getFormattedDuration())
            setTextViewText(
                R.id.textParkingLocation,
                record.location ?: "位置未记录"
            )
            setTextViewText(
                R.id.textParkingStartTime,
                "开始时间: ${formatTime(record.startTime)}"
            )
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_car)
                .setContentTitle("停车中 · ${record.getFormattedDuration()}")
                .setContentText(record.location ?: "查看详情")
                .setContentIntent(mainPendingIntent)
                .setOngoing(true)
                .setUsesChronometer(true)  // 使用系统计时器（部分厂商支持动态更新）
                .setWhen(record.startTime)
                .setCustomContentView(customView)
                .addAction(
                    R.drawable.ic_shield_paused,
                    "结束停车",
                    stopPendingIntent
                )
        } else {
            Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_car)
                .setContentTitle("停车中 · ${record.getFormattedDuration()}")
                .setContentText(record.location ?: "查看详情")
                .setContentIntent(mainPendingIntent)
                .setOngoing(true)
                .setCustomContentView(customView)
                .addAction(
                    R.drawable.ic_shield_paused,
                    "结束停车",
                    stopPendingIntent
                )
        }

        return builder.build()
    }

    private fun createEmptyNotification(channelId: String): Notification {
        return Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle("停车计时")
            .setContentText("正在启动...")
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    override fun onDestroy() {
        updateTask?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1003
        private const val ACTION_STOP = "com.nuolemo.app.ACTION_STOP_PARKING"
        private const val ACTION_UPDATE = "com.nuolemo.app.ACTION_UPDATE_PARKING"
        private const val UPDATE_INTERVAL = 60000L  // 每分钟更新一次

        fun start(context: Context) {
            val intent = Intent(context, ParkingTimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ParkingTimerService::class.java)
            context.stopService(intent)
        }
    }
}
