package com.ondy.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ondy.app.util.ScheduleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Restores scheduled summary alarms after a reboot or an app update.
 *
 * Alarms are cleared by the system on reboot, so every saved schedule must be
 * re-registered. [goAsync] keeps the process alive until the work is done while
 * avoiding blocking the main thread.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleManager: ScheduleManager

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(TIMEOUT_MS) {
                    scheduleManager.rescheduleAllAlarms()
                } ?: Log.w(TAG, "Rescheduling alarms timed out")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms", e)
            } finally {
                runCatching { pendingResult.finish() }
                    .onFailure { Log.w(TAG, "Broadcast already finished", it) }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val TIMEOUT_MS = 8_000L
    }
}
