package com.ondy.app.domain.model

/**
 * Portable snapshot of everything the user can back up: which apps are blocked
 * and when summary notifications are scheduled.
 *
 * Notification history is intentionally excluded. It is transient, potentially
 * sensitive, and can grow large, so it is never written to a backup file.
 */
data class BackupData(
    val selectedApps: List<String>,
    val scheduleTimes: List<ScheduleTime>
) {
    val isEmpty: Boolean
        get() = selectedApps.isEmpty() && scheduleTimes.isEmpty()
}
