package com.smarthome.app.ui.auth

import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.domain.usecase.auth.ConfirmSignUpUseCase
import com.smarthome.core.domain.usecase.auth.SignInUseCase
import com.smarthome.core.domain.usecase.auth.SignUpUseCase
import com.smarthome.core.error.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val emailInput: String = "",
    val passwordInput: String = "",
    val nameInput: String = "",
    val otpInput: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val confirmSignUpUseCase: ConfirmSignUpUseCase
) : BaseViewModel<AuthState>(AuthState()) {

    fun onEmailChanged(email: String) = setState { it.copy(emailInput = email) }
    fun onPasswordChanged(password: String) = setState { it.copy(passwordInput = password) }
    fun onNameChanged(name: String) = setState { it.copy(nameInput = name) }
    fun onOtpChanged(otp: String) = setState { it.copy(otpInput = otp) }

    fun login() {
        val state = uiState.value
        if (state.emailInput.isBlank() || state.passwordInput.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Please fill all fields"))
            return
        }

        setState { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = signInUseCase(state.emailInput, state.passwordInput)) {
                is DomainResult.Success -> {
                    setState { it.copy(isLoading = false) }
                    sendEvent(UiEvent.Navigate("dashboard"))
                }
                is DomainResult.Error -> {
                    setState { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
                }
            }
        }
    }

    fun register() {
        val state = uiState.value
        if (state.emailInput.isBlank() || state.passwordInput.isBlank() || state.nameInput.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Please fill all fields"))
            return
        }

        setState { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = signUpUseCase(state.emailInput, state.passwordInput, state.nameInput)) {
                is DomainResult.Success -> {
                    setState { it.copy(isLoading = false) }
                    sendEvent(UiEvent.Navigate("otp"))
                }
                is DomainResult.Error -> {
                    setState { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
                }
            }
        }
    }

    fun verifyOtp() {
        val state = uiState.value
        if (state.otpInput.isBlank()) return

        setState { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = confirmSignUpUseCase(state.emailInput, state.otpInput)) {
                is DomainResult.Success -> {
                    setState { it.copy(isLoading = false) }
                    // Auto login after verification
                    login()
                }
                is DomainResult.Error -> {
                    setState { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
                }
            }
        }
    }
}
