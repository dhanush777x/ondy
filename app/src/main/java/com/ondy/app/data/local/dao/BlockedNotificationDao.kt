package com.ondy.app.data.local.dao

import androidx.room.*
import com.ondy.app.data.local.entity.BlockedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNotificationDao {
    @Query("SELECT * FROM blocked_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<BlockedNotificationEntity>>

    @Query("SELECT COUNT(*) FROM blocked_notifications")
    suspend fun getNotificationCount(): Int

    @Query("SELECT * FROM blocked_notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BlockedNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: BlockedNotificationEntity): Long

    @Update
    suspend fun update(notification: BlockedNotificationEntity)

    @Query("DELETE FROM blocked_notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM blocked_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)
}