package com.nuolemo.app

/**
 * 当前停车记录
 */
data class ParkingRecord(
    val startTime: Long,
    val location: String? = null,
    val photoPath: String? = null,
    val reminderMinutes: Int = 0,  // 0 表示不提醒
    val reminderScheduled: Boolean = false
) {
    /**
     * 计算已停车时长（毫秒）
     */
    fun getDuration(): Long {
        return System.currentTimeMillis() - startTime
    }

    /**
     * 格式化停车时长
     */
    fun getFormattedDuration(): String {
        val duration = getDuration()
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "刚刚"
        }
    }
}

/**
 * 停车历史记录
 */
data class ParkingHistory(
    val startTime: Long,
    val endTime: Long,
    val location: String? = null,
    val photoPath: String? = null
) {
    /**
     * 计算停车时长（毫秒）
     */
    fun getDuration(): Long {
        return endTime - startTime
    }

    /**
     * 格式化停车时长
     */
    fun getFormattedDuration(): String {
        val duration = getDuration()
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "不足1分钟"
        }
    }
}
