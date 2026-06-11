package com.smarthome.core.repository.device

import com.smarthome.core.error.DomainResult
import com.smarthome.core.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>
    fun observeDevice(deviceId: String): Flow<Device?>
    suspend fun toggleSwitch(deviceId: String, switchIndex: Int, targetState: Boolean): DomainResult<Unit>
    suspend fun syncDevices(): DomainResult<Unit>
    suspend fun removeDevice(deviceId: String): DomainResult<Unit>
}
