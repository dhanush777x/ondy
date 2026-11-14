package com.ondy.app.domain.repository

import com.ondy.app.domain.model.BackupData
import com.ondy.app.domain.model.BlockedNotification
import com.ondy.app.domain.model.ScheduleTime
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val notifications: Flow<List<BlockedNotification>>
    val history: Flow<List<BlockedNotification>>
    suspend fun getNotificationById(id: Long): BlockedNotification?
    suspend fun saveNotification(notification: BlockedNotification): Long
    suspend fun deleteNotification(id: Long)
    suspend fun deleteAllNotifications()
}

interface ScheduleRepository {
    fun getAllScheduleTimes(): Flow<List<ScheduleTime>>
    suspend fun getAllScheduleTimesList(): List<ScheduleTime>
    suspend fun addScheduleTime(scheduleTime: ScheduleTime): Long
    suspend fun deleteScheduleTime(id: Long)
}

interface SelectedAppRepository {
    fun getSelectedApps(): Flow<List<String>>
    suspend fun getSelectedAppsList(): List<String>
    suspend fun toggleAppSelection(packageName: String)
    suspend fun isAppSelected(packageName: String): Boolean
}

interface BackupRepository {
    /** Reads the current selection and schedule into a portable snapshot. */
    suspend fun createBackup(): BackupData

    /**
     * Atomically replaces the selection and schedule with [data]. Cancelling
     * and rescheduling of alarms is the caller's responsibility because it
     * needs the [com.ondy.app.util.ScheduleManager].
     */
    suspend fun restoreBackup(data: BackupData)
}
