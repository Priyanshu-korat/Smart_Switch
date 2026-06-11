package com.smarthome.core.network

import com.smarthome.core.database.dao.NotificationDao
import com.smarthome.core.database.entity.NotificationEntity
import com.smarthome.core.error.DomainResult
import com.smarthome.core.network.dto.NotificationDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSyncManager @Inject constructor(
    private val apiService: ApiService,
    private val notificationDao: NotificationDao
) {
    suspend fun syncNotifications(): DomainResult<Unit> {
        val result = safeApiCall { apiService.getNotifications() }
        return when (result) {
            is DomainResult.Success -> {
                val notifications = result.data.items
                notifications.forEach { dto ->
                    notificationDao.insertNotification(dto.toEntity())
                }
                Timber.d("NotificationSyncManager: Synced ${notifications.size} notifications")
                DomainResult.Success(Unit)
            }
            is DomainResult.Error -> result
        }
    }

    suspend fun markRead(notificationId: String): DomainResult<Unit> {
        // Optimistic local update
        notificationDao.markAsRead(notificationId)
        // Then sync to server
        return safeApiCall { apiService.markNotificationRead(notificationId) }
    }

    private fun NotificationDto.toEntity() = NotificationEntity(
        id = id,
        timestamp = timestamp,
        category = category,
        title = title,
        message = message,
        deviceId = deviceId,
        isRead = isRead
    )
}
