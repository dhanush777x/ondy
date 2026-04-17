package com.ondy.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_notifications", indices = [androidx.room.Index(value = ["notificationKey"], unique = true)])
data class BlockedNotificationEntity(
    @PrimaryKey(autoGenerate = true)
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

@Entity(tableName = "schedule_times")
data class ScheduleTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int
)

@Entity(tableName = "selected_apps")
data class SelectedAppEntity(
    @PrimaryKey
    val packageName: String
)