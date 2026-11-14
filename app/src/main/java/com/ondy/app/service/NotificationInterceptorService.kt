package com.ondy.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ondy.app.MainActivity
import com.ondy.app.R
import com.ondy.app.data.preferences.PreferencesManager
import com.ondy.app.domain.model.BlockedNotification
import com.ondy.app.domain.repository.NotificationRepository
import com.ondy.app.domain.repository.SelectedAppRepository
import com.ondy.app.util.AppCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Intercepts notifications from user-selected applications.
 *
 * Design notes:
 *  - [selectedPackages] is an in-memory snapshot kept up to date from Room, so the
 *    hot path never touches the database.
 *  - [recentKeys] maps eventKey (sbn.key:postTime) to claim time. This allows genuine
 *    updates to the same notification (new postTime) to pass while collapsing true
 *    duplicate deliveries (same key + same postTime).
 *  - Progress/ongoing/system notifications are filtered out by category/flags — they
 *    are dismissed from the shade but never stored.
 *  - The service optionally promotes itself to a foreground service, which is what
 *    keeps the listener alive on aggressive OEM battery managers.
 */
@AndroidEntryPoint
class NotificationInterceptorService : NotificationListenerService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var selectedAppRepository: SelectedAppRepository

    @Inject
    lateinit var appCache: AppCache

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var foregroundJob: Job? = null
    private var cleanupJob: Job? = null

    @Volatile
    private var selectedPackages: Set<String> = emptySet()

    // recentKeys maps eventKey (sbn.key:postTime) -> claim timestamp.
    // Using postTime in the key ensures each notify() call is a distinct event,
    // so updates to the same chat (same sbn.key, new postTime) are stored.
    private val recentKeys = ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()

        observeSelectedPackages()
        observeForegroundPreference()
        warmAppCache()
        startCleanupLoop()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Listener disconnected, requesting rebind")
        // Some OEMs drop the listener without rebinding it automatically.
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                requestRebind(ComponentName(this, NotificationInterceptorService::class.java))
            }.onFailure { Log.w(TAG, "Rebind request failed", it) }
        }, REBIND_DELAY_MS)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return

        val packageName = statusBarNotification.packageName
        if (packageName !in selectedPackages) return

        // Group summaries are containers for child notifications. We don't store them,
        // but we must dismiss them or the empty group lingers in the shade.
        if (statusBarNotification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            statusBarNotification.key?.let(::cancelSafely)
            return
        }

        // Skip storage for progress/ongoing/system notifications — they are transient UI,
        // not messages the user wants to review later. Still dismiss them from the shade.
        if (shouldSkipStorage(statusBarNotification.notification)) {
            statusBarNotification.key?.let(::cancelSafely)
            return
        }

        // sbn.key is unique per user/package/id/tag and never blank on supported APIs.
        val uniqueKey = statusBarNotification.key ?: return
        val postTime = statusBarNotification.postTime
        val now = System.currentTimeMillis()

        // Event key includes postTime so each notify() call is a distinct event.
        // This allows updates to the same notification (same sbn.key, new postTime)
        // to be stored, while true duplicate deliveries (same key + same postTime)
        // are collapsed.
        val eventKey = "${uniqueKey}:${postTime}"

        // Atomically claim this event so concurrent posts cannot be saved twice.
        if (recentKeys.putIfAbsent(eventKey, now) != null) return

        try {
            val appName = appCache.getAppName(packageName)
            val notification = BlockedNotification.fromStatusBar(
                statusBarNotification,
                appName,
                ::storePendingIntent
            )

            // Remove the notification from the shade as soon as possible.
            cancelSafely(uniqueKey)

            serviceScope.launch {
                try {
                    notificationRepository.saveNotification(notification)
                    Log.d(TAG, "Saved notification from $packageName: ${notification.title}")
                } catch (e: Exception) {
                    // Allow a future repost of the same event to be retried.
                    recentKeys.remove(eventKey, now)
                    Log.e(TAG, "Error saving notification", e)
                }
            }
        } catch (e: Exception) {
            recentKeys.remove(eventKey, now)
            Log.e(TAG, "Error processing notification from $packageName", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val uniqueKey = sbn?.key ?: return
        val postTime = sbn?.postTime ?: 0L
        val eventKey = "${uniqueKey}:${postTime}"
        recentKeys.remove(eventKey)
    }

    override fun onDestroy() {
        cleanupJob?.cancel()
        foregroundJob?.cancel()
        serviceScope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun observeSelectedPackages() {
        serviceScope.launch {
            selectedAppRepository.getSelectedApps()
                .distinctUntilChanged()
                .catch { Log.e(TAG, "Failed to observe selected apps", it) }
                .collect { packages ->
                    selectedPackages = packages.toSet()
                    Log.d(TAG, "Selected packages updated (${packages.size})")
                }
        }
    }

    private fun observeForegroundPreference() {
        foregroundJob = serviceScope.launch {
            preferencesManager.runInForeground
                .catch { Log.e(TAG, "Failed to observe foreground preference", it) }
                .collectLatest { enabled ->
                    if (enabled) promoteToForeground() else demoteFromForeground()
                }
        }
    }

    private fun warmAppCache() {
        serviceScope.launch {
            runCatching { appCache.ensureLoaded() }
                .onFailure { Log.w(TAG, "Failed to warm app cache", it) }
        }
    }

    private fun startCleanupLoop() {
        cleanupJob = serviceScope.launch {
            while (true) {
                delay(CLEANUP_INTERVAL_MS)
                val now = System.currentTimeMillis()
                recentKeys.entries.removeIf { (_, time) -> now - time > KEY_TTL_MS }
                cleanupOldPendingIntents()
            }
        }
    }

    private fun promoteToForeground() {
        runCatching {
            createForegroundChannel()
            startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
            Log.i(TAG, "Promoted to foreground service")
        }.onFailure {
            // Background-start restrictions can reject the promotion on some
            // OEM builds; the listener still works while bound, so never crash.
            Log.w(TAG, "Unable to promote to foreground service", it)
        }
    }

    private fun demoteFromForeground() {
        runCatching {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            Log.i(TAG, "Demoted from foreground service")
        }.onFailure { Log.w(TAG, "Unable to demote foreground service", it) }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.foreground_service_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createForegroundChannel() {
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            getString(R.string.foreground_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.foreground_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun cancelSafely(key: String) {
        runCatching { cancelNotification(key) }
            .onFailure { Log.w(TAG, "Could not cancel notification $key", it) }
    }

    /** Returns true for notifications that should be dismissed but not stored. */
    private fun shouldSkipStorage(notification: Notification): Boolean {
        return notification.category == Notification.CATEGORY_PROGRESS
            || notification.category == Notification.CATEGORY_SERVICE
            || notification.category == Notification.CATEGORY_SYSTEM
            || notification.category == Notification.CATEGORY_TRANSPORT
            || (notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0)
    }

    companion object {
        private const val TAG = "NotiInterceptor"
        private const val FOREGROUND_CHANNEL_ID = "ondy_foreground_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 1002
        private const val REBIND_DELAY_MS = 2_000L
        private const val CLEANUP_INTERVAL_MS = 60_000L
        private const val KEY_TTL_MS = 5 * 60_000L

        // PendingIntents cannot be persisted, so they are kept for the lifetime of
        // the process and referenced by id from the notification history.
        private val pendingIntents = ConcurrentHashMap<Long, PendingIntent>()
        private val pendingIntentTimestamps = ConcurrentHashMap<Long, Long>()
        private var nextPendingIntentId = 0L

        @Synchronized
        fun storePendingIntent(contentIntent: PendingIntent): Long {
            val id = ++nextPendingIntentId
            pendingIntents[id] = contentIntent
            pendingIntentTimestamps[id] = System.currentTimeMillis()
            return id
        }

        fun getPendingIntent(id: Long): PendingIntent? = pendingIntents[id]

        fun removePendingIntent(id: Long) {
            pendingIntents.remove(id)
            pendingIntentTimestamps.remove(id)
        }

        fun clearPendingIntents() {
            pendingIntents.clear()
            pendingIntentTimestamps.clear()
        }

        private fun cleanupOldPendingIntents() {
            val now = System.currentTimeMillis()
            pendingIntentTimestamps.entries
                .removeIf { (_, time) -> now - time > PENDING_INTENT_TTL_MS }
            pendingIntents.keys.retainAll(pendingIntentTimestamps.keys)
        }

        private const val PENDING_INTENT_TTL_MS = 60 * 60_000L
    }
}
