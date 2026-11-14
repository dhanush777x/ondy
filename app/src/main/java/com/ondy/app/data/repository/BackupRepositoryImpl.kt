package com.ondy.app.data.repository

import androidx.room.withTransaction
import com.ondy.app.data.local.OndyDatabase
import com.ondy.app.data.local.dao.ScheduleTimeDao
import com.ondy.app.data.local.dao.SelectedAppDao
import com.ondy.app.data.local.entity.ScheduleTimeEntity
import com.ondy.app.data.local.entity.SelectedAppEntity
import com.ondy.app.domain.model.BackupData
import com.ondy.app.domain.model.ScheduleTime
import com.ondy.app.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: OndyDatabase,
    private val scheduleTimeDao: ScheduleTimeDao,
    private val selectedAppDao: SelectedAppDao
) : BackupRepository {

    override suspend fun createBackup(): BackupData {
        return BackupData(
            selectedApps = selectedAppDao.getAllSelectedAppsList().map { it.packageName },
            scheduleTimes = scheduleTimeDao.getAllScheduleTimesList().map { entity ->
                ScheduleTime(hour = entity.hour, minute = entity.minute)
            }
        )
    }

    /**
     * Deletes and re-inserts both tables inside one transaction, so a failure
     * halfway through can never leave the user with a half-restored setup.
     */
    override suspend fun restoreBackup(data: BackupData) {
        val apps = data.selectedApps
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map(::SelectedAppEntity)
            .toList()

        val times = data.scheduleTimes
            .asSequence()
            .filter { it.hour in 0..23 && it.minute in 0..59 }
            .distinctBy { it.hour to it.minute }
            .map { ScheduleTimeEntity(hour = it.hour, minute = it.minute) }
            .toList()

        database.withTransaction {
            selectedAppDao.deleteAll()
            if (apps.isNotEmpty()) selectedAppDao.insertAll(apps)

            scheduleTimeDao.deleteAll()
            if (times.isNotEmpty()) scheduleTimeDao.insertAll(times)
        }
    }
}
