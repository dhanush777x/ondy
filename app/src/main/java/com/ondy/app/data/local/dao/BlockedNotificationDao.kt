package com.ondy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.ondy.app.data.local.entity.BlockedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNotificationDao {
    @Query("SELECT * FROM blocked_notifications ORDER BY id DESC")
    fun getAllNotifications(): Flow<List<BlockedNotificationEntity>>

    @Query("SELECT * FROM blocked_notifications WHERE showInHistory = 1 ORDER BY id DESC")
    fun getHistory(): Flow<List<BlockedNotificationEntity>>

    @Query("SELECT * FROM blocked_notifications WHERE showInWidget = 1 ORDER BY id DESC")
    fun getWidget(): Flow<List<BlockedNotificationEntity>>

    @Query("SELECT COUNT(*) FROM blocked_notifications")
    suspend fun getNotificationCount(): Int

    @Query("SELECT * FROM blocked_notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BlockedNotificationEntity?

    @Upsert
    suspend fun upsert(notification: BlockedNotificationEntity): Long

    @Delete
    suspend fun delete(notification: BlockedNotificationEntity)

    @Query("DELETE FROM blocked_notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM blocked_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Deletes everything except the newest [maxRows] rows in a single statement. */
    @Query(
        """
        DELETE FROM blocked_notifications
        WHERE id NOT IN (
            SELECT id FROM blocked_notifications ORDER BY id DESC LIMIT :maxRows
        )
        """
    )
    suspend fun pruneTo(maxRows: Int)
}
