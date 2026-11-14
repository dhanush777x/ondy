package com.ondy.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ondy.app.data.local.dao.BlockedNotificationDao
import com.ondy.app.data.local.dao.ScheduleTimeDao
import com.ondy.app.data.local.dao.SelectedAppDao
import com.ondy.app.data.local.entity.BlockedNotificationEntity
import com.ondy.app.data.local.entity.ScheduleTimeEntity
import com.ondy.app.data.local.entity.SelectedAppEntity

/**
 * Room database for Ondy.
 *
 * Version history:
 *  - 2: initial release
 *  - 3: added filter/history/widget columns
 *  - 4: notificationKey became non-null
 *  - 5: schema cleanup; all pre-release databases are recreated
 *
 * Ondy has not shipped to any app store yet, so instead of maintaining a chain of
 * fragile migrations we use [RoomDatabase.Builder.fallbackToDestructiveMigration].
 * Any database created by an older pre-release build is recreated on upgrade.
 * Before the first public release this should be replaced by real migrations and
 * schema export should be re-enabled.
 */
@Database(
    entities = [
        BlockedNotificationEntity::class,
        ScheduleTimeEntity::class,
        SelectedAppEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class OndyDatabase : RoomDatabase() {
    abstract fun blockedNotificationDao(): BlockedNotificationDao
    abstract fun scheduleTimeDao(): ScheduleTimeDao
    abstract fun selectedAppDao(): SelectedAppDao

    companion object {
        private const val DATABASE_NAME = "ondy_db"

        fun createDatabase(context: Context): OndyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OndyDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
