package com.nuolemo.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMatcherTest {
    @Test
    fun `matches strict chinese move car keyword`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(),
                sender = "1069",
                body = "您的车辆挡住了别人，请及时挪车。",
            )

        assertTrue(result.matched)
        assertTrue(result.reason.contains("挪车"))
    }

    @Test
    fun `matches move car wording without trusted sender`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(),
                sender = "10086",
                body = "请车主移车，出口被您挡住了。",
            )

        assertTrue(result.matched)
        assertTrue(result.reason.contains("移车"))
    }

    @Test
    fun `matches hubei traffic police real sms`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(plateNumbers = listOf("鄂W8608J")),
                sender = "湖北交警",
                body = "【湖北交警】您的小型汽车鄂W8608J于2026年7月9日7时19分在流芳路高新四路至高新六路未按规定停放已被记录，请立即驶离，未及时驶离的，将依法予以处罚。",
            )

        assertTrue(result.matched)
    }

    @Test
    fun `does not match verification code text`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(),
                sender = "1069",
                body = "验证码 123456，五分钟内有效。",
            )

        assertFalse(result.matched)
    }

    @Test
    fun `matches normalized plate number`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(plateNumbers = listOf("鄂A12345")),
                sender = "交管平台",
                body = "鄂 A12345 请挪车，车辆挡道。",
            )

        assertTrue(result.matched)
    }

    @Test
    fun `matches when custom keywords configured`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(keywords = listOf("地库堵门")),
                sender = "物业",
                body = "地库堵门，请联系车主尽快处理。",
            )

        assertTrue(result.matched)
        assertTrue(result.reason.contains("地库堵门"))
    }

    @Test
    fun `empty custom keywords still uses strict defaults`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(keywords = emptyList()),
                sender = "交管12123",
                body = "您的车辆妨碍通行，请立即驶离。",
            )

        assertTrue(result.matched)
    }

    @Test
    fun `does not overmatch generic car owner text`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(keywords = emptyList()),
                sender = "银行",
                body = "尊敬的车主，您的积分即将过期。",
            )

        assertFalse(result.matched)
    }

    @Test
    fun `empty keyword list still allows plate matching`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(keywords = emptyList(), plateNumbers = listOf("沪B-88888")),
                sender = null,
                body = "车辆沪 B88888，请尽快处理。",
            )

        assertTrue(result.matched)
    }

    @Test
    fun `ignores plate fragments that are too short`() {
        val result =
            SmsMatcher.matchWithReason(
                settings = sampleSettings(keywords = emptyList(), plateNumbers = listOf("123456")),
                sender = "银行",
                body = "验证码 123456，五分钟内有效。",
            )

        assertFalse(result.matched)
    }

    private fun sampleSettings(
        keywords: List<String> = emptyList(),
        plateNumbers: List<String> = emptyList(),
    ): AppSettings {
        return AppSettings(
            enabled = true,
            keywords = keywords,
            plateNumbers = plateNumbers,
            alarmDurationSeconds = 60,
            vibrate = true,
        )
    }
}
