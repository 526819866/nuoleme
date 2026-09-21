package com.nuolemo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = SettingsStore.load(context)
        if (!settings.enabled) {
            return
        }

        // 方式1：从广播中获取短信（正常系统）
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNotEmpty()) {
                val sender = messages.firstOrNull()?.displayOriginatingAddress ?: messages.firstOrNull()?.originatingAddress
                val body = messages.joinToString(separator = "") { message ->
                    message.displayMessageBody ?: message.messageBody ?: ""
                }.trim()

                if (body.isNotEmpty()) {
                    EventLogger.log(context, "收到广播短信", "发送者: $sender")
                    processSms(context, settings, sender, body, "广播")
                    return
                }
            }
        }

        // 方式2：查询短信数据库最新一条（小米等系统兜底）
        // 注：这个查询只在广播触发时执行，不是轮询
        checkLatestSms(context, settings)
    }

    private fun checkLatestSms(context: Context, settings: AppSettings) {
        try {
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                    if (addressIndex >= 0 && bodyIndex >= 0 && dateIndex >= 0) {
                        val sender = it.getString(addressIndex) ?: ""
                        val body = it.getString(bodyIndex) ?: ""
                        val date = it.getLong(dateIndex)

                        // 只处理最近30秒内的短信
                        val now = System.currentTimeMillis()
                        if (now - date < 30000) {
                            EventLogger.log(context, "查询到新短信", "发送者: $sender")
                            processSms(context, settings, sender, body, "数据库")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "查询短信数据库失败", e)
            EventLogger.log(context, "短信查询失败", e.message ?: "未知错误")
        }
    }

    private fun processSms(
        context: Context,
        settings: AppSettings,
        sender: String?,
        body: String,
        source: String
    ) {
        val matchResult = SmsMatcher.matchWithReason(settings, sender, body)

        val triggered = if (matchResult.matched) {
            AlarmLaunchHelper.startAlarm(context, sender, body)
            true
        } else {
            false
        }

        EventLogger.logSmsReceived(context, sender, body, matchResult, triggered)
        EventLogger.log(context, "短信处理完成", "来源: $source, 触发: $triggered")
    }
}
