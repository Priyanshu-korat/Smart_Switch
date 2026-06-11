package com.smarthome.core.domain.usecase.device

import com.smarthome.core.error.DomainResult
import com.smarthome.core.repository.device.DeviceRepository
import javax.inject.Inject

class ToggleSwitchUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        switchIndex: Int,
        targetState: Boolean
    ): DomainResult<Unit> {
        return deviceRepository.toggleSwitch(deviceId, switchIndex, targetState)
    }
}
