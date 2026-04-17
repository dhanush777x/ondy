package com.ondy.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ondy.app.domain.model.BlockedNotification
import com.ondy.app.domain.repository.NotificationRepository
import com.ondy.app.domain.repository.SelectedAppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class NotificationInterceptorService : NotificationListenerService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var selectedAppRepository: SelectedAppRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var selectedPackages: Set<String> = emptySet()
    private val storedNotificationKeys = ConcurrentHashMap<String, Long>()
    private var notificationIdCounter = 0L
    private val TAG = "NotiInterceptor"
    
    private val appSelectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_REFRESH_APPS) {
                loadSelectedAppsSync()
            }
        }
    }
    
    companion object {
        const val ACTION_REFRESH_APPS = "com.ondy.app.REFRESH_APPS"
        private val pendingIntentsMap = ConcurrentHashMap<Long, android.app.PendingIntent>()
        private val pendingIntentTimestamps = ConcurrentHashMap<Long, Long>()
        
        fun storePendingIntent(id: Long, pendingIntent: android.app.PendingIntent) {
            pendingIntentsMap[id] = pendingIntent
            pendingIntentTimestamps[id] = System.currentTimeMillis()
        }

        fun getPendingIntent(id: Long): android.app.PendingIntent? {
            return pendingIntentsMap[id]
        }

        fun removePendingIntent(id: Long) {
            pendingIntentsMap.remove(id)
            pendingIntentTimestamps.remove(id)
        }

        fun cleanupOldPendingIntents() {
            val now = System.currentTimeMillis()
            pendingIntentTimestamps.entries.removeIf { (_, time) ->
                now - time > 3600000
            }
            val currentIds = pendingIntentTimestamps.keys.toSet()
            pendingIntentsMap.keys.toList().forEach { key ->
                if (key !in currentIds) {
                    pendingIntentsMap.remove(key)
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        loadSelectedAppsSync()
        registerReceiver(appSelectionReceiver, IntentFilter(ACTION_REFRESH_APPS))
        
        serviceScope.launch {
            while (true) {
                delay(60000)
                val now = System.currentTimeMillis()
                storedNotificationKeys.entries.removeIf { (_, time) ->
                    now - time > 300000
                }
                cleanupOldPendingIntents()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(appSelectionReceiver)
    }

    private fun loadSelectedAppsSync() {
        try {
            val job = serviceScope.launch {
                selectedPackages = selectedAppRepository.getSelectedAppsList().toSet()
            }
            runBlocking {
                job.join()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load selected apps", e)
            selectedPackages = emptySet()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName

        if (packageName in selectedPackages) {
            try {
                val notificationKey = sbn.key
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val content = extras.getCharSequence("android.text")?.toString() ?: ""
                
                storedNotificationKeys[notificationKey] = System.currentTimeMillis()
                
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }

                val contentIntent = sbn.notification.contentIntent
                
                val storedId = if (contentIntent != null) {
                    synchronized(this) {
                        val id = ++notificationIdCounter
                        storePendingIntent(id, contentIntent)
                        id
                    }
                } else null

                val finalTitle = title.ifBlank {
                    extras.getCharSequence("android.subText")?.toString() ?: ""
                }
                val finalContent = content.ifBlank {
                    extras.getCharSequence("android.infoText")?.toString() ?: ""
                }

                serviceScope.launch {
                    val notification = BlockedNotification(
                        packageName = packageName,
                        appName = appName,
                        title = finalTitle,
                        content = finalContent,
                        timestamp = System.currentTimeMillis(),
                        intentAction = storedId?.toString(),
                        intentData = if (contentIntent != null) "pendingIntent" else null,
                        notificationKey = notificationKey
                    )
                    notificationRepository.saveNotification(notification)
                }

                cancelNotification(sbn.key)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from $packageName", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        loadSelectedAppsSync()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val key = sbn.key
        storedNotificationKeys.remove(key)
    }
}