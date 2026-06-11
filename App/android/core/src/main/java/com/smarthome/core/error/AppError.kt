package com.smarthome.core.error

sealed class AppError : Exception() {
    abstract val userMessage: String

    sealed class NetworkError : AppError() {
        data object NoConnection : NetworkError() {
            override val userMessage = "No internet connection."
        }
        data object Timeout : NetworkError() {
            override val userMessage = "Request timed out."
        }
    }

    sealed class AuthError : AppError() {
        data object SessionExpired : AuthError() {
            override val userMessage = "Session expired. Please log in again."
        }
        data class InvalidCredentials(override val userMessage: String = "Invalid email or password.") : AuthError()
    }

    sealed class MqttError : AppError() {
        data object CommandTimeout : MqttError() {
            override val userMessage = "Device didn't respond. It might be offline."
        }
        data object ConnectionLost : MqttError() {
            override val userMessage = "Lost connection to the smart home network."
        }
    }

    sealed class ProvisioningError : AppError() {
        data object ApNotFound : ProvisioningError() {
            override val userMessage = "Could not find the device's setup network."
        }
        data object WrongPassword : ProvisioningError() {
            override val userMessage = "Incorrect Wi-Fi password for the device."
        }
    }

    data class Unknown(val originalException: Throwable? = null) : AppError() {
        override val userMessage = "An unexpected error occurred."
    }
}
