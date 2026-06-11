package com.smarthome.core.network

import com.smarthome.core.database.dao.DeviceDao
import com.smarthome.core.database.entity.DeviceEntity
import com.smarthome.core.database.entity.DeviceStateEntity
import com.smarthome.core.error.DomainResult
import com.smarthome.core.network.dto.DeviceDto
import com.smarthome.core.time.TimeProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs remote data (API) → local Room DB.
 * Called on app startup and on pull-to-refresh.
 * The UI always reads from Room (single source of truth).
 */
@Singleton
class SyncManager @Inject constructor(
    private val apiService: ApiService,
    private val deviceDao: DeviceDao,
    private val timeProvider: TimeProvider
) {
    /**
     * Pulls devices + their switches from the API and merges into Room.
     * Uses REPLACE strategy — stale devices are preserved (removed only by explicit delete).
     */
    suspend fun syncDevices(): DomainResult<Unit> {
        val result = safeApiCall { apiService.getDevices() }
        return when (result) {
            is DomainResult.Success -> {
                val devices = result.data.items
                devices.forEach { dto ->
                    deviceDao.insertOrUpdateDevice(dto.toEntity())
                    Timber.d("SyncManager: Synced device ${dto.deviceId}")
                }
                DomainResult.Success(Unit)
            }
            is DomainResult.Error -> result
        }
    }

    private fun DeviceDto.toEntity() = DeviceEntity(
        id = deviceId,
        name = name,
        lastSeenAt = lastSeenAt,
        isOnline = isOnline,
        isClaimed = true,
        role = role
    )
}
