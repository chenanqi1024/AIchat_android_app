package vibe.ccc.aichat.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatters {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日")

    fun relativeChatTime(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return "刚刚"
        val instant = runCatching { Instant.parse(createdAt) }.getOrNull() ?: return "刚刚"
        val zoneId = ZoneId.systemDefault()
        val dateTime = instant.atZone(zoneId)
        val today = LocalDate.now(zoneId)
        val date = dateTime.toLocalDate()

        return when {
            date == today -> timeFormatter.format(dateTime)
            date == today.minusDays(1) -> "昨天 ${timeFormatter.format(dateTime)}"
            else -> dateFormatter.format(dateTime)
        }
    }
}
