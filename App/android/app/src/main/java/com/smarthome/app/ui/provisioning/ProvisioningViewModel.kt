package com.smarthome.app.ui.provisioning

import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.error.DomainResult
import com.smarthome.core.provisioning.ProvisioningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProvisioningState(
    val isLoading: Boolean = false,
    val ssidInput: String = "",
    val passwordInput: String = "",
    val currentStep: ProvisioningStep = ProvisioningStep.Instructions
)

enum class ProvisioningStep {
    Instructions,
    ConnectToDevice,
    EnterCredentials,
    Provisioning,
    Success
}

@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    private val provisioningRepository: ProvisioningRepository
) : BaseViewModel<ProvisioningState>(ProvisioningState()) {

    fun onSsidChanged(ssid: String) = setState { it.copy(ssidInput = ssid) }
    fun onPasswordChanged(password: String) = setState { it.copy(passwordInput = password) }

    fun goToStep(step: ProvisioningStep) = setState { it.copy(currentStep = step) }

    fun startProvisioning() {
        val state = uiState.value
        if (state.ssidInput.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Please enter your Wi-Fi network name"))
            return
        }

        setState { it.copy(isLoading = true, currentStep = ProvisioningStep.Provisioning) }

        viewModelScope.launch {
            val result = provisioningRepository.provisionDevice(
                ssid = state.ssidInput,
                wifiPassword = state.passwordInput,
                mqttHost = "your-hivemq-host.s1.eu.hivemq.cloud",
                mqttPort = 8883,
                mqttUser = "smarthome_device",
                mqttPass = "device_password_here"
            )

            setState { it.copy(isLoading = false) }

            when (result) {
                is DomainResult.Success -> {
                    setState { it.copy(currentStep = ProvisioningStep.Success) }
                    sendEvent(UiEvent.ShowSnackbar("Device added successfully!"))
                }
                is DomainResult.Error -> {
                    setState { it.copy(currentStep = ProvisioningStep.EnterCredentials) }
                    sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
                }
            }
        }
    }
}
