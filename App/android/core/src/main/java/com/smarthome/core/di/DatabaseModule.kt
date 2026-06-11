package com.smarthome.core.di

import android.content.Context
import androidx.room.Room
import com.smarthome.core.constants.DatabaseConstants
import com.smarthome.core.database.AppDatabase
import com.smarthome.core.database.dao.DeviceDao
import com.smarthome.core.database.dao.NotificationDao
import com.smarthome.core.database.dao.PendingCommandDao
import com.smarthome.core.database.dao.SceneDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DatabaseConstants.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    fun provideSceneDao(database: AppDatabase): SceneDao = database.sceneDao()

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun providePendingCommandDao(database: AppDatabase): PendingCommandDao {
        return database.pendingCommandDao()
    }
}
