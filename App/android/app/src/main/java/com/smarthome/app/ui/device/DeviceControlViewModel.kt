package com.smarthome.app.ui.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.domain.usecase.device.ToggleSwitchUseCase
import com.smarthome.core.error.DomainResult
import com.smarthome.core.repository.device.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val toggleSwitchUseCase: ToggleSwitchUseCase
) : BaseViewModel<DeviceControlUiState>(DeviceControlUiState()) {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])

    init {
        observeDevice()
    }

    private fun observeDevice() {
        viewModelScope.launch {
            deviceRepository.observeDevice(deviceId).collectLatest { device ->
                setState {
                    it.copy(
                        isLoading = false,
                        device = device,
                        error = if (device == null) "Device not found" else null
                    )
                }
            }
        }
    }

    fun onToggleSwitch(switchIndex: Int, currentState: Boolean) {
        viewModelScope.launch {
            val result = toggleSwitchUseCase(deviceId, switchIndex, !currentState)
            if (result is DomainResult.Error) {
                sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
            }
        }
    }

    fun onBackClick() = sendEvent(UiEvent.NavigateUp)
}
