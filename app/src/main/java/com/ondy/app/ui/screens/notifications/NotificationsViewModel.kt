package com.ondy.app.ui.screens.notifications

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ondy.app.domain.model.BlockedNotification
import com.ondy.app.domain.repository.NotificationRepository
import com.ondy.app.service.NotificationInterceptorService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<BlockedNotification> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    val packageManager: PackageManager = application.packageManager

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getAllNotifications().collect { notifications ->
                _uiState.value = _uiState.value.copy(
                    notifications = notifications,
                    isLoading = false
                )
            }
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            val notification = notificationRepository.getNotificationById(id)
            notification?.let {
                val pendingIntentId = it.intentAction?.toLongOrNull()
                pendingIntentId?.let { pid ->
                    NotificationInterceptorService.removePendingIntent(pid)
                }
            }
            notificationRepository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.getAllNotifications().collect { notifications ->
                notifications.forEach { notification ->
                    val pendingIntentId = notification.intentAction?.toLongOrNull()
                    pendingIntentId?.let { pid ->
                        NotificationInterceptorService.removePendingIntent(pid)
                    }
                }
                notificationRepository.deleteAllNotifications()
            }
        }
    }

    fun createNotificationIntent(notification: BlockedNotification): Intent? {
        return try {
            val pendingIntentId = notification.intentAction?.toLongOrNull()
            
            if (pendingIntentId != null && notification.intentData == "pendingIntent") {
                val pendingIntent = NotificationInterceptorService.getPendingIntent(pendingIntentId)
                if (pendingIntent != null) {
                    try {
                        pendingIntent.send()
                        return null
                    } catch (e: android.app.PendingIntent.CanceledException) {
                    } catch (e: Exception) {
                    }
                }
            }

            val intent = if (!notification.intentAction.isNullOrBlank()) {
                Intent(notification.intentAction)
            } else {
                Intent(Intent.ACTION_VIEW)
            }

            notification.intentData?.let { 
                if (it != "pendingIntent") {
                    intent.data = android.net.Uri.parse(it)
                }
            }

            if (intent.component == null) {
                intent.setPackage(notification.packageName)
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            if (packageManager.resolveActivity(intent, 0) != null) {
                intent
            } else {
                val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launchIntent
            }
        } catch (e: Exception) {
            null
        }
    }
}