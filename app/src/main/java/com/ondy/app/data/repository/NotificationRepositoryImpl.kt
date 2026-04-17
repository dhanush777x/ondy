package com.ondy.app.data.repository

import com.ondy.app.data.local.dao.BlockedNotificationDao
import com.ondy.app.data.local.entity.BlockedNotificationEntity
import com.ondy.app.domain.model.BlockedNotification
import com.ondy.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val blockedNotificationDao: BlockedNotificationDao
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<BlockedNotification>> {
        return blockedNotificationDao.getAllNotifications().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getNotificationById(id: Long): BlockedNotification? {
        return blockedNotificationDao.getById(id)?.toDomain()
    }

    override suspend fun saveNotification(notification: BlockedNotification): Long {
        return blockedNotificationDao.insert(notification.toEntity())
    }

    override suspend fun deleteNotification(id: Long) {
        blockedNotificationDao.deleteById(id)
    }

    override suspend fun deleteAllNotifications() {
        blockedNotificationDao.deleteAll()
    }

    private fun BlockedNotificationEntity.toDomain(): BlockedNotification {
        return BlockedNotification(
            id = id,
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            timestamp = timestamp,
            isRead = isRead,
            intentAction = intentAction,
            intentData = intentData,
            notificationKey = notificationKey
        )
    }

    private fun BlockedNotification.toEntity(): BlockedNotificationEntity {
        return BlockedNotificationEntity(
            id = id,
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            timestamp = timestamp,
            isRead = isRead,
            intentAction = intentAction,
            intentData = intentData,
            notificationKey = notificationKey
        )
    }
}