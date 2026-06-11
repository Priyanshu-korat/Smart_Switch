package com.smarthome.app.ui.auth

import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.error.DomainResult
import com.smarthome.core.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashState(
    val isLoading: Boolean = true
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<SplashState>(SplashState()) {

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Small delay for branding visibility
            delay(1000)
            
            // Cold-launch check
            val isLoggedIn = authRepository.getAuthState().first()
            if (isLoggedIn) {
                // To Dashboard
                sendEvent(UiEvent.Navigate("dashboard"))
            } else {
                // To Login
                sendEvent(UiEvent.Navigate("login"))
            }
        }
    }
}
