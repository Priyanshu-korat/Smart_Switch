package com.smarthome.app.ui.device

import com.smarthome.core.model.Device

data class DeviceControlUiState(
    val isLoading: Boolean = true,
    val device: Device? = null,
    val error: String? = null
)
