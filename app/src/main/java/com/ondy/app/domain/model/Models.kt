package com.ondy.app.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSelected: Boolean = false,
    val isUserApp: Boolean = true
)

data class ScheduleTime(
    val id: Long = 0,
    val hour: Int,
    val minute: Int
) {
    fun toDisplayString(): String {
        val hourDisplay = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format("%d:%02d %s", hourDisplay, minute, amPm)
    }
}

data class BlockedNotification(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val intentAction: String? = null,
    val intentData: String? = null,
    val notificationKey: String? = null
)