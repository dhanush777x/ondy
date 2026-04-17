package com.ondy.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ondy.app.util.ScheduleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleManager: ScheduleManager

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleManager.rescheduleAllAlarms()
        }
    }
}