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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "ScheduleManager"
    
    fun scheduleNotification(scheduleTime: ScheduleTime) {
        val intent = Intent(context, ScheduledNotificationReceiver::class.java)
        val requestCode = (scheduleTime.hour * 100 + scheduleTime.minute)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, scheduleTime.hour)
            set(Calendar.MINUTE, scheduleTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.cancel(pendingIntent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule alarm: missing permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for ${scheduleTime.hour}:${scheduleTime.minute}", e)
        }
    }

    suspend fun cancelNotification(id: Long) {
        val scheduleTime = scheduleRepository.getAllScheduleTimesList().find { it.id == id }
        
        scheduleTime?.let { time ->
            val intent = Intent(context, ScheduledNotificationReceiver::class.java)
            val requestCode = (time.hour * 100 + time.minute)
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
        }
    }

    fun rescheduleAllAlarms() {
        scope.launch {
            try {
                val scheduleTimes = scheduleRepository.getAllScheduleTimesList()
                scheduleTimes.forEach { time ->
                    scheduleNotification(time)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule all alarms", e)
            }
        }
    }
}