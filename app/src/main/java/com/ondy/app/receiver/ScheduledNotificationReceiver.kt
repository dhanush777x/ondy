package com.ondy.app.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ondy.app.MainActivity
import com.ondy.app.OndyApp
import com.ondy.app.R
import com.ondy.app.domain.model.ScheduleTime
import com.ondy.app.domain.repository.NotificationRepository
import com.ondy.app.domain.repository.ScheduleRepository
import com.ondy.app.util.ScheduleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Fires when a scheduled summary is due, schedules the next day's occurrence and
 * posts a summary notification with the number of intercepted notifications.
 */
@AndroidEntryPoint
class ScheduledNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var scheduleManager: ScheduleManager

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    override fun onReceive(context: Context, intent: Intent?) {
        val hour = intent?.getIntExtra(EXTRA_HOUR, -1) ?: -1
        val minute = intent?.getIntExtra(EXTRA_MINUTE, -1) ?: -1

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Re-arm before doing any work so a failure below cannot stop
                // future summaries from firing. A time that was deleted (or
                // replaced by an import) must not resurrect itself, so only
                // re-arm when it still exists in the database. If the lookup
                // itself fails, keep the old behaviour and re-arm.
                if (hour in 0..23 && minute in 0..59) {
                    val savedTimes = runCatching {
                        withTimeoutOrNull(GO_ASYNC_TIMEOUT_MS) {
                            scheduleRepository.getAllScheduleTimesList()
                        }
                    }.getOrNull()

                    val stillScheduled = savedTimes == null ||
                        savedTimes.any { it.hour == hour && it.minute == minute }

                    if (stillScheduled) {
                        scheduleManager.scheduleNotification(
                            ScheduleTime(hour = hour, minute = minute)
                        )
                    } else {
                        Log.i(TAG, "Schedule $hour:$minute was removed, not re-arming")
                    }
                }

                if (!canPostNotifications(context)) {
                    Log.w(TAG, "Notification permission not granted, skipping summary")
                    return@launch
                }

                val notifications = withTimeoutOrNull(GO_ASYNC_TIMEOUT_MS) {
                    notificationRepository.notifications.first()
                }.orEmpty()

                if (notifications.isNotEmpty()) {
                    showSummaryNotification(context, notifications.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling scheduled notification", e)
            } finally {
                runCatching { pendingResult.finish() }
                    .onFailure { Log.w(TAG, "Broadcast already finished", it) }
            }
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Permission is checked in canPostNotifications() before this method is called;
    // lint cannot follow the helper, so the check is documented here as well.
    @SuppressLint("MissingPermission")
    private fun showSummaryNotification(context: Context, count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, OndyApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.resources.getQuantityString(
                R.plurals.summary_notification_text,
                count,
                count
            ))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.resources.getQuantityString(
                        R.plurals.summary_notification_big_text,
                        count,
                        count
                    )
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }.onFailure { Log.e(TAG, "Unable to post summary notification", it) }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"

        private const val TAG = "ScheduledNotifReceiver"
        private const val REQUEST_CODE = 1001
        private const val GO_ASYNC_TIMEOUT_MS = 8_000L
    }
}
