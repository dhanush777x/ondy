package com.ondy.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ondy.app.domain.model.ScheduleTime
import com.ondy.app.domain.repository.ScheduleRepository
import com.ondy.app.receiver.ScheduledNotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns scheduling of the recurring summary notification.
 *
 * Alarms are exact when the platform allows it and fall back to inexact
 * while-idle alarms otherwise. Every time an alarm fires,
 * [ScheduledNotificationReceiver] asks this class to schedule the next day's
 * occurrence, so summaries keep running without reopening the app.
 */
@Singleton
class ScheduleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(scheduleTime: ScheduleTime) {
        val pendingIntent = pendingIntentFor(scheduleTime.hour, scheduleTime.minute)
        val triggerAt = nextOccurrenceMs(scheduleTime.hour, scheduleTime.minute)

        alarmManager.cancel(pendingIntent)
        setAlarm(triggerAt, pendingIntent)
    }

    fun cancelNotification(scheduleTime: ScheduleTime) {
        alarmManager.cancel(pendingIntentFor(scheduleTime.hour, scheduleTime.minute))
    }

    suspend fun cancelNotification(id: Long) {
        scheduleRepository.getAllScheduleTimesList()
            .firstOrNull { it.id == id }
            ?.let(::cancelNotification)
    }

    /** Re-registers every saved alarm. Called after boot and app upgrades. */
    suspend fun rescheduleAllAlarms() {
        runCatching {
            scheduleRepository.getAllScheduleTimesList().forEach(::scheduleNotification)
        }.onFailure { Log.e(TAG, "Failed to reschedule alarms", it) }
    }

    /**
     * Cancels every alarm currently stored in the database. Used before an
     * import replaces the schedule so removed times cannot keep firing.
     */
    suspend fun cancelAllAlarms() {
        runCatching {
            scheduleRepository.getAllScheduleTimesList().forEach(::cancelNotification)
        }.onFailure { Log.e(TAG, "Failed to cancel alarms", it) }
    }

    private fun setAlarm(triggerAtMs: Long, pendingIntent: PendingIntent) {
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (exactAllowed) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm denied, falling back to inexact", e)
            }
        }

        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }.onFailure { Log.e(TAG, "Failed to schedule alarm at $triggerAtMs", it) }
    }

    private fun nextOccurrenceMs(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    private fun pendingIntentFor(hour: Int, minute: Int): PendingIntent {
        val intent = Intent(context, ScheduledNotificationReceiver::class.java).apply {
            putExtra(ScheduledNotificationReceiver.EXTRA_HOUR, hour)
            putExtra(ScheduledNotificationReceiver.EXTRA_MINUTE, minute)
        }
        return PendingIntent.getBroadcast(
            context,
            hour * 100 + minute,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "ScheduleManager"
    }
}
