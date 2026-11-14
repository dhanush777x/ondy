package com.ondy.app.domain.model

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.StatusBarNotification
import kotlin.math.abs

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSelected: Boolean = false,
    val isUserApp: Boolean = true,
    val iconResId: Int = 0
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
    val notificationKey: String? = null,
    // Extensibility fields for the future filter/action system.
    val filterId: Long? = null,
    val showInHistory: Boolean = true,
    val showInWidget: Boolean = false
) {
    companion object {
        private const val MAX_CONTENT_LENGTH = 500

        fun fromStatusBar(
            sbn: StatusBarNotification,
            appName: String,
            storePendingIntent: (PendingIntent) -> Long
        ): BlockedNotification {
            val extras = sbn.notification.extras

            val title = sequenceOf(
                Notification.EXTRA_TITLE,
                Notification.EXTRA_TITLE_BIG,
                Notification.EXTRA_SUB_TEXT,
                Notification.EXTRA_INFO_TEXT
            ).mapNotNull { extras.getCharSequence(it)?.toString()?.trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()

            val content = sequenceOf(
                Notification.EXTRA_TEXT,
                Notification.EXTRA_BIG_TEXT,
                Notification.EXTRA_SUMMARY_TEXT,
                Notification.EXTRA_INFO_TEXT,
                Notification.EXTRA_SUB_TEXT
            ).mapNotNull { extras.getCharSequence(it)?.toString()?.trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()

            val contentIntent = sbn.notification.contentIntent
            val storedId = contentIntent?.let(storePendingIntent)

            return BlockedNotification(
                packageName = sbn.packageName,
                appName = appName,
                title = title.ifBlank { appName }.take(MAX_CONTENT_LENGTH),
                content = content.ifBlank { "New notification" }.take(MAX_CONTENT_LENGTH),
                timestamp = sbn.postTime,
                intentAction = storedId?.toString(),
                intentData = if (contentIntent != null) "pendingIntent" else null,
                // sbn.key is unique per user/package/id/tag, but apps reuse it when
                // they update a notification (a second message in the same chat,
                // for example). The post time makes the key unique per posting so
                // each genuine update is stored, while a duplicate delivery of the
                // exact same event still collides on the unique index.
                notificationKey = "${sbn.key}:${sbn.postTime}"
            )
        }
    }

    /**
     * Time-tolerant equality used to collapse notifications an app reposts with
     * a new key within [windowMs] (for example progress-style updates).
     */
    fun matches(other: BlockedNotification, windowMs: Long = 10_000L): Boolean {
        return packageName == other.packageName &&
            title == other.title &&
            content == other.content &&
            filterId == other.filterId &&
            abs(timestamp - other.timestamp) <= windowMs
    }
}
