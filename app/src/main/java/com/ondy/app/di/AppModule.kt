package com.ondy.app.di

import android.content.Context
import androidx.room.Room
import com.ondy.app.data.local.OndyDatabase
import com.ondy.app.data.local.dao.BlockedNotificationDao
import com.ondy.app.data.local.dao.ScheduleTimeDao
import com.ondy.app.data.local.dao.SelectedAppDao
import com.ondy.app.data.repository.NotificationRepositoryImpl
import com.ondy.app.data.repository.ScheduleRepositoryImpl
import com.ondy.app.data.repository.SelectedAppRepositoryImpl
import com.ondy.app.domain.repository.NotificationRepository
import com.ondy.app.domain.repository.ScheduleRepository
import com.ondy.app.domain.repository.SelectedAppRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OndyDatabase {
        return Room.databaseBuilder(
            context,
            OndyDatabase::class.java,
            "ondy_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBlockedNotificationDao(database: OndyDatabase): BlockedNotificationDao {
        return database.blockedNotificationDao()
    }

    @Provides
    fun provideScheduleTimeDao(database: OndyDatabase): ScheduleTimeDao {
        return database.scheduleTimeDao()
    }

    @Provides
    fun provideSelectedAppDao(database: OndyDatabase): SelectedAppDao {
        return database.selectedAppDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        impl: ScheduleRepositoryImpl
    ): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindSelectedAppRepository(
        impl: SelectedAppRepositoryImpl
    ): SelectedAppRepository
}