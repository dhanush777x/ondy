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

    companion object {
        const val MAX_HISTORY = 200
    }

    override val notifications: Flow<List<BlockedNotification>>
        get() = blockedNotificationDao.getAllNotifications()
            .map { list -> list.map(::toDomain) }

    override val history: Flow<List<BlockedNotification>>
        get() = blockedNotificationDao.getHistory()
            .map { list -> list.map(::toDomain) }

    override suspend fun getNotificationById(id: Long): BlockedNotification? {
        return blockedNotificationDao.getById(id)?.let(::toDomain)
    }

    override suspend fun saveNotification(notification: BlockedNotification): Long {
        val id = blockedNotificationDao.upsert(notification.toEntity())
        blockedNotificationDao.pruneTo(MAX_HISTORY)
        return id
    }

    override suspend fun deleteNotification(id: Long) {
        blockedNotificationDao.deleteById(id)
    }

    override suspend fun deleteAllNotifications() {
        blockedNotificationDao.deleteAll()
    }

    private fun toDomain(entity: BlockedNotificationEntity): BlockedNotification {
        return BlockedNotification(
            id = entity.id,
            packageName = entity.packageName,
            appName = entity.appName,
            title = entity.title,
            content = entity.content,
            timestamp = entity.timestamp,
            isRead = entity.isRead,
            intentAction = entity.intentAction,
            intentData = entity.intentData,
            notificationKey = entity.notificationKey,
            filterId = entity.filterId,
            showInHistory = entity.showInHistory,
            showInWidget = entity.showInWidget
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
                ?: "unknown_${System.currentTimeMillis()}_$id",
            filterId = filterId,
            showInHistory = showInHistory,
            showInWidget = showInWidget
        )
    }
}
