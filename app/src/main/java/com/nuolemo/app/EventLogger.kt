package com.nuolemo.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsEvent(
    val timestamp: Long,
    val sender: String?,
    val bodyPreview: String,
    val matchResult: MatchResult,
    val triggered: Boolean,
)

data class MatchResult(
    val matched: Boolean,
    val reason: String,
)

object EventLogger {
    private const val PREFS_NAME = "nuolemo_event_log"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 50
    private const val BODY_PREVIEW_LENGTH = 60

    fun logSmsReceived(
        context: Context,
        sender: String?,
        body: String,
        matchResult: MatchResult,
        triggered: Boolean,
    ) {
        val event = SmsEvent(
            timestamp = System.currentTimeMillis(),
            sender = sender,
            bodyPreview = body.take(BODY_PREVIEW_LENGTH),
            matchResult = matchResult,
            triggered = triggered,
        )

        val events = loadEvents(context).toMutableList()
        events.add(0, event)
        if (events.size > MAX_EVENTS) {
            events.subList(MAX_EVENTS, events.size).clear()
        }

        saveEvents(context, events)
    }

    fun loadEvents(context: Context): List<SmsEvent> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_EVENTS, null) ?: return emptyList()

        return try {
            deserializeEvents(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearEvents(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_EVENTS)
            .apply()
    }

    /**
     * 记录调试信息（用于排查问题）
     */
    fun log(context: Context, tag: String, message: String) {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val timeStr = dateFormat.format(Date(timestamp))
        android.util.Log.d("EventLogger", "[$timeStr] $tag: $message")
    }

    fun formatEvent(event: SmsEvent): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        val time = dateFormat.format(Date(event.timestamp))
        val senderText = event.sender ?: "未知"
        val status = if (event.triggered) "✓ 已触发" else "✗ 未触发"
        return "$time | $senderText\n$status | ${event.matchResult.reason}\n${event.bodyPreview}"
    }

    private fun serializeEvents(events: List<SmsEvent>): String {
        return events.joinToString(separator = "\n###\n") { event ->
            "${event.timestamp}|${event.sender.orEmpty()}|${event.bodyPreview}|" +
                "${event.matchResult.matched}|${event.matchResult.reason}|${event.triggered}"
        }
    }

    private fun deserializeEvents(json: String): List<SmsEvent> {
        return json.split("\n###\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size == 6) {
                    try {
                        SmsEvent(
                            timestamp = parts[0].toLong(),
                            sender = parts[1].ifEmpty { null },
                            bodyPreview = parts[2],
                            matchResult = MatchResult(
                                matched = parts[3].toBoolean(),
                                reason = parts[4],
                            ),
                            triggered = parts[5].toBoolean(),
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
    }

    private fun saveEvents(context: Context, events: List<SmsEvent>) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EVENTS, serializeEvents(events))
            .apply()
    }
}
