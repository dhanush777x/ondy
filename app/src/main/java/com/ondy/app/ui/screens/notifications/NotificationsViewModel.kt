package com.ondy.app.ui.screens.notifications

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ondy.app.domain.model.BlockedNotification
import com.ondy.app.domain.repository.NotificationRepository
import com.ondy.app.service.NotificationInterceptorService
import com.ondy.app.util.AppCache
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<BlockedNotification> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val appCache: AppCache,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val packageManager = application.packageManager

    init {
        viewModelScope.launch {
            notificationRepository.notifications
                .catch { Log.e(TAG, "Failed to observe notifications", it) }
                .collect { notifications ->
                    _uiState.update {
                        it.copy(notifications = notifications, isLoading = false)
                    }
                }
        }
        viewModelScope.launch { runCatching { appCache.ensureLoaded() } }
    }

    fun iconModel(packageName: String): String? = appCache.getIconModel(packageName)

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            runCatching {
                notificationRepository.getNotificationById(id)
                    ?.intentAction
                    ?.toLongOrNull()
                    ?.let(NotificationInterceptorService::removePendingIntent)
                notificationRepository.deleteNotification(id)
            }.onFailure { Log.e(TAG, "Error deleting notification", it) }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            runCatching {
                notificationRepository.deleteAllNotifications()
                NotificationInterceptorService.clearPendingIntents()
            }.onFailure { Log.e(TAG, "Error clearing notifications", it) }
        }
    }

    /**
     * Replays the original notification's action when possible, otherwise opens
     * the app that posted it. Returns null when the action was already sent.
     */
    fun createNotificationIntent(notification: BlockedNotification): Intent? {
        return try {
            val pendingIntentId = notification.intentAction?.toLongOrNull()

            if (pendingIntentId != null && notification.intentData == INTENT_DATA_PENDING) {
                val pendingIntent =
                    NotificationInterceptorService.getPendingIntent(pendingIntentId)
                if (pendingIntent != null) {
                    try {
                        pendingIntent.send()
                        return null
                    } catch (e: android.app.PendingIntent.CanceledException) {
                        Log.w(TAG, "PendingIntent cancelled", e)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error sending PendingIntent", e)
                    }
                }
            }

            val intent = if (!notification.intentAction.isNullOrBlank()) {
                Intent(notification.intentAction)
            } else {
                Intent(Intent.ACTION_VIEW)
            }

            notification.intentData?.let {
                if (it != INTENT_DATA_PENDING) {
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
                packageManager.getLaunchIntentForPackage(notification.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification intent", e)
            null
        }
    }

    companion object {
        private const val TAG = "NotificationsVM"
        private const val INTENT_DATA_PENDING = "pendingIntent"
    }
}
