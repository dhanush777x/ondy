package com.ondy.app.data.local.dao

import androidx.room.*
import com.ondy.app.data.local.entity.ScheduleTimeEntity
import com.ondy.app.data.local.entity.SelectedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTimeDao {
    @Query("SELECT * FROM schedule_times ORDER BY hour, minute")
    fun getAllScheduleTimes(): Flow<List<ScheduleTimeEntity>>

    @Query("SELECT * FROM schedule_times ORDER BY hour, minute")
    suspend fun getAllScheduleTimesList(): List<ScheduleTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheduleTime: ScheduleTimeEntity): Long

    @Query("DELETE FROM schedule_times WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SelectedAppDao {
    @Query("SELECT * FROM selected_apps")
    fun getAllSelectedApps(): Flow<List<SelectedAppEntity>>

    @Query("SELECT * FROM selected_apps")
    suspend fun getAllSelectedAppsList(): List<SelectedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: SelectedAppEntity)

    @Query("DELETE FROM selected_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM selected_apps WHERE packageName = :packageName)")
    suspend fun isAppSelected(packageName: String): Boolean
}