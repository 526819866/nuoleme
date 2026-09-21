package com.nuolemo.app

import java.util.Locale

object SmsMatcher {
    private const val MIN_PLATE_TOKEN_LENGTH = 7

    @Suppress("UNUSED_PARAMETER")
    fun matches(settings: AppSettings, sender: String?, body: String): Boolean {
        return matchWithReason(settings, sender, body).matched
    }

    fun matchWithReason(settings: AppSettings, sender: String?, body: String): MatchResult {
        val normalizedBody = body.trim()
        if (normalizedBody.isEmpty()) {
            return MatchResult(false, "短信内容为空")
        }

        val keywordPool = SettingsStore.activeKeywords(settings.keywords)
        val matchedKeyword = keywordPool.firstOrNull { normalizedBody.contains(it, ignoreCase = true) }
        if (matchedKeyword != null) {
            return MatchResult(true, "命中关键词: $matchedKeyword")
        }

        val compactBody = normalizePlateText(normalizedBody)
        val compactPlates =
            settings.plateNumbers
                .map(::normalizePlate)
                .filter { it.length >= MIN_PLATE_TOKEN_LENGTH }
        val matchedPlate = compactPlates.firstOrNull { compactBody.contains(it) }
        if (matchedPlate != null) {
            return MatchResult(true, "命中车牌: $matchedPlate")
        }

        return MatchResult(false, "未匹配任何关键词或车牌")
    }

    internal fun normalizePlate(rawValue: String): String {
        return rawValue
            .uppercase(Locale.ROOT)
            .filterNot {
                it.isWhitespace() ||
                    it == '-' ||
                    it == ':' ||
                    it == '：' ||
                    it == '·' ||
                    it == '•' ||
                    it == '.'
            }
    }

    private fun normalizePlateText(body: String): String = normalizePlate(body)
}
