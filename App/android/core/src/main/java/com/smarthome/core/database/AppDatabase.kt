package com.smarthome.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smarthome.core.database.dao.DeviceDao
import com.smarthome.core.database.dao.NotificationDao
import com.smarthome.core.database.dao.PendingCommandDao
import com.smarthome.core.database.dao.SceneDao
import com.smarthome.core.database.entity.DeviceEntity
import com.smarthome.core.database.entity.DeviceStateEntity
import com.smarthome.core.database.entity.FavoriteEntity
import com.smarthome.core.database.entity.NotificationEntity
import com.smarthome.core.database.entity.PendingClaimEntity
import com.smarthome.core.database.entity.PendingCommandEntity
import com.smarthome.core.database.entity.SceneEntity

@Database(
    entities = [
        DeviceEntity::class,
        DeviceStateEntity::class,
        SceneEntity::class,
        NotificationEntity::class,
        PendingClaimEntity::class,
        FavoriteEntity::class,
        PendingCommandEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun sceneDao(): SceneDao
    abstract fun notificationDao(): NotificationDao
    abstract fun pendingCommandDao(): PendingCommandDao
}
