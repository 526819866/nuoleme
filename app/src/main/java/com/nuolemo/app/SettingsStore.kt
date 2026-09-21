package com.nuolemo.app

import android.content.Context
import java.util.Locale

enum class AlarmMode {
    SOUND_AND_VIBRATE,  // 响铃+震动
    SOUND_ONLY,         // 仅响铃
    VIBRATE_ONLY        // 仅震动
}

data class AppSettings(
    val enabled: Boolean,
    val keywords: List<String>,
    val plateNumbers: List<String>,
    val alarmDurationSeconds: Int,
    val vibrate: Boolean,
    val enhancedMode: Boolean = false,
    val alarmMode: AlarmMode = AlarmMode.SOUND_AND_VIBRATE,
)

object SettingsStore {
    private const val PREFS_NAME = "nuolemo_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_KEYWORDS = "keywords"
    private const val KEY_PLATE_NUMBERS = "plate_numbers"
    private const val KEY_ALARM_DURATION_SECONDS = "alarm_duration_seconds"
    private const val KEY_VIBRATE = "vibrate"
    private const val KEY_ENHANCED_MODE = "enhanced_mode"
    private const val KEY_ALARM_MODE = "alarm_mode"
    private const val LEGACY_KEY_MAXIMIZE_VOLUME = "maximize_volume"

    val defaultKeywords: List<String> =
        listOf(
            "挪车",
            "移车",
            "一键挪车",
            "妨碍通行",
            "请立即驶离",
            "请及时驶离",
            "车辆挡道",
            "车辆妨碍",
        )

    fun load(context: Context): AppSettings {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 处理旧版本的 vibrate 设置迁移到新的 alarmMode
        val legacyVibrate = prefs.getBoolean(KEY_VIBRATE, true)
        val alarmModeString = prefs.getString(KEY_ALARM_MODE, null)
        val alarmMode = if (alarmModeString != null) {
            try {
                AlarmMode.valueOf(alarmModeString)
            } catch (e: IllegalArgumentException) {
                if (legacyVibrate) AlarmMode.SOUND_AND_VIBRATE else AlarmMode.SOUND_ONLY
            }
        } else {
            // 首次运行，根据旧的 vibrate 设置决定默认值
            if (legacyVibrate) AlarmMode.SOUND_AND_VIBRATE else AlarmMode.SOUND_ONLY
        }

        return AppSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            keywords = sanitizeCustomKeywords(
                prefs.getString(KEY_KEYWORDS, "").orEmpty(),
            ),
            plateNumbers = sanitizePlateNumbers(
                prefs.getString(KEY_PLATE_NUMBERS, "").orEmpty(),
            ),
            alarmDurationSeconds = normalizeDuration(
                prefs.getInt(KEY_ALARM_DURATION_SECONDS, 60),
            ),
            vibrate = legacyVibrate,
            enhancedMode = prefs.getBoolean(KEY_ENHANCED_MODE, false),
            alarmMode = alarmMode,
        )
    }

    fun save(context: Context, settings: AppSettings) {
        val sanitizedKeywords = sanitizeCustomKeywords(formatEditorInput(settings.keywords))
        val sanitizedPlates = sanitizePlateNumbers(formatEditorInput(settings.plateNumbers))
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putString(KEY_KEYWORDS, formatEditorInput(sanitizedKeywords))
            .putString(KEY_PLATE_NUMBERS, formatEditorInput(sanitizedPlates))
            .putInt(KEY_ALARM_DURATION_SECONDS, normalizeDuration(settings.alarmDurationSeconds))
            .putBoolean(KEY_VIBRATE, settings.vibrate)
            .putBoolean(KEY_ENHANCED_MODE, settings.enhancedMode)
            .putString(KEY_ALARM_MODE, settings.alarmMode.name)
            .remove(LEGACY_KEY_MAXIMIZE_VOLUME)
            .apply()
    }

    fun sanitizeKeywords(rawInput: String): List<String> = splitAndClean(rawInput)

    fun activeKeywords(customKeywords: List<String>): List<String> {
        return (defaultKeywords + customKeywords).distinctBy { it.lowercase(Locale.ROOT) }
    }

    fun sanitizePlateNumbers(rawInput: String): List<String> =
        splitAndClean(rawInput).map { it.uppercase(Locale.ROOT) }

    fun formatEditorInput(values: List<String>): String = values.joinToString(separator = "\n")

    private fun sanitizeCustomKeywords(rawInput: String): List<String> {
        return sanitizeKeywords(rawInput).filterNot { keyword ->
            defaultKeywords.any { defaultKeyword -> defaultKeyword.equals(keyword, ignoreCase = true) }
        }
    }

    private fun splitAndClean(rawInput: String): List<String> {
        val seen = linkedSetOf<String>()
        rawInput
            .split("\n", ",", "，", ";", "；")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { seen += it }
        return seen.toList()
    }

    private fun normalizeDuration(rawValue: Int): Int {
        return when (rawValue) {
            0, 30, 60, 120 -> rawValue
            else -> 60
        }
    }
}
