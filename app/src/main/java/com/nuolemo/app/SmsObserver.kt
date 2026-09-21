package com.nuolemo.app

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log

/**
 * 短信数据库监听器 - 增强模式
 *
 * 用于解决小米等系统不发送短信广播的问题。
 * 当短信数据库有新记录时，查询最新短信并触发处理。
 */
class SmsObserver(
    private val context: Context,
    private val onSmsReceived: (sender: String?, body: String, timestamp: Long) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var lastProcessedTimestamp: Long = 0

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        Log.d(TAG, "短信数据库变化检测")
        queryLatestSms()
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "短信数据库变化: $uri")
        queryLatestSms()
    }

    private fun queryLatestSms() {
        try {
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                    if (bodyIndex >= 0 && dateIndex >= 0) {
                        val sender = if (addressIndex >= 0) it.getString(addressIndex) else null
                        val body = it.getString(bodyIndex) ?: ""
                        val timestamp = it.getLong(dateIndex)

                        // 防止重复处理
                        if (timestamp > lastProcessedTimestamp) {
                            lastProcessedTimestamp = timestamp

                            // 只处理5秒内的新短信
                            val now = System.currentTimeMillis()
                            if (now - timestamp <= 5000) {
                                Log.d(TAG, "检测到新短信: ${body.take(20)}... (${now - timestamp}ms前)")
                                onSmsReceived(sender, body, timestamp)
                            } else {
                                Log.d(TAG, "忽略旧短信: ${(now - timestamp) / 1000}秒前")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "查询短信失败", e)
        }
    }

    companion object {
        private const val TAG = "SmsObserver"
    }
}
