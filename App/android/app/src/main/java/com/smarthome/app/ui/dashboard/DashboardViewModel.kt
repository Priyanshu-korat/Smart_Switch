package com.smarthome.app.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.domain.usecase.device.ToggleSwitchUseCase
import com.smarthome.core.error.DomainResult
import com.smarthome.core.mqtt.MqttConnectionState
import com.smarthome.core.mqtt.MqttManager
import com.smarthome.core.repository.device.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val mqttManager: MqttManager,
    private val toggleSwitchUseCase: ToggleSwitchUseCase
) : BaseViewModel<DashboardUiState>(DashboardUiState()) {

    init {
        loadData()
        observeMqttState()
    }

    private fun loadData() {
        viewModelScope.launch {
            setState { it.copy(isLoading = true, greeting = buildGreeting()) }
            deviceRepository.observeDevices().collectLatest { devices ->
                val activeCount = devices.sumOf { d -> d.switches.count { s -> s.state } }
                setState {
                    it.copy(
                        isLoading = false,
                        devices = devices,
                        activeCount = activeCount
                    )
                }
            }
        }
    }

    private fun observeMqttState() {
        viewModelScope.launch {
            mqttManager.connectionState.collectLatest { state ->
                setState { it.copy(mqttState = state) }
                if (state == MqttConnectionState.Reconnecting) {
                    sendEvent(UiEvent.ShowSnackbar("Reconnecting to your devices…"))
                }
            }
        }
    }

    fun onToggleSwitch(deviceId: String, switchIndex: Int, currentState: Boolean) {
        viewModelScope.launch {
            val result = toggleSwitchUseCase(deviceId, switchIndex, !currentState)
            if (result is DomainResult.Error) {
                sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
            }
        }
    }

    fun onAddDeviceClick() = sendEvent(UiEvent.Navigate("provisioning"))

    fun onDeviceCardClick(deviceId: String) = sendEvent(UiEvent.Navigate("device/$deviceId"))

    private fun buildGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
