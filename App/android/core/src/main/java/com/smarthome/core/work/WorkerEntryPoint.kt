package com.smarthome.core.work

import com.smarthome.core.database.dao.PendingCommandDao
import com.smarthome.core.mqtt.MqttPublisher
import com.smarthome.core.network.NotificationSyncManager
import com.smarthome.core.network.SyncManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * EntryPoint to allow WorkManager workers to inject dependencies
 * without requiring the androidx.hilt:hilt-work compiler plugin.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun mqttPublisher(): MqttPublisher
    fun pendingCommandDao(): PendingCommandDao
    fun syncManager(): SyncManager
    fun notificationSyncManager(): NotificationSyncManager
}
