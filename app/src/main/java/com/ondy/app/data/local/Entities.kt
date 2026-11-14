package com.ondy.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_notifications",
    indices = [Index(value = ["notificationKey"], unique = true)]
)
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
    // notificationKey includes postTime (sbn.key:postTime) to distinguish
    // genuine updates from duplicate deliveries of the same event.
    val notificationKey: String,
    val filterId: Long? = null,
    val showInHistory: Boolean = true,
    val showInWidget: Boolean = false
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