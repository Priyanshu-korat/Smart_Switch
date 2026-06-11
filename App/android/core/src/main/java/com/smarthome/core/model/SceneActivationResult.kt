package com.smarthome.core.model

sealed class SceneActivationResult {
    data object Success : SceneActivationResult()
    data class PartialSuccess(val failedDevices: List<String>) : SceneActivationResult()
    data class Failed(val reason: String) : SceneActivationResult()
}
