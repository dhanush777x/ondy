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

@Database(
    entities = [
        BlockedNotificationEntity::class,
        ScheduleTimeEntity::class,
        SelectedAppEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class OndyDatabase : RoomDatabase() {
    abstract fun blockedNotificationDao(): BlockedNotificationDao
    abstract fun scheduleTimeDao(): ScheduleTimeDao
    abstract fun selectedAppDao(): SelectedAppDao
    
    companion object {
        @Volatile
        private var INSTANCE: OndyDatabase? = null
        
        fun getInstance(context: Context): OndyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OndyDatabase::class.java,
                    "ondy_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}