package com.smarthome.app.ui.dashboard

import com.smarthome.core.model.Device
import com.smarthome.core.mqtt.MqttConnectionState

data class DashboardUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
    val mqttState: MqttConnectionState = MqttConnectionState.Disconnected,
    val greeting: String = "Good evening",
    val activeCount: Int = 0
)
