package com.smarthome.app.ui.scenes

import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.model.Scene
import com.smarthome.core.model.SceneActivationResult
import com.smarthome.core.mqtt.MqttPublisher
import com.smarthome.core.mqtt.SwitchCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScenesViewModel @Inject constructor(
    private val mqttPublisher: MqttPublisher
) : BaseViewModel<ScenesUiState>(ScenesUiState()) {

    init {
        // Scenes would be loaded from SceneRepository (Room DB synced from API)
        // For now we load placeholder data
        setState { it.copy(isLoading = false, scenes = sampleScenes()) }
    }

    fun onActivateScene(scene: Scene) {
        if (uiState.value.activatingSceneId != null) return // prevent double-tap

        setState { it.copy(activatingSceneId = scene.id) }

        viewModelScope.launch {
            val commands = scene.targets.map { target ->
                SwitchCommand(
                    deviceId = target.deviceId,
                    switchIndex = target.switchIndex,
                    targetState = target.targetState
                )
            }

            val result = mqttPublisher.sendSceneCommands(commands)
            setState { it.copy(activatingSceneId = null, lastResult = result) }

            when (result) {
                is SceneActivationResult.Success ->
                    sendEvent(UiEvent.ShowSnackbar("\"${scene.name}\" activated!"))
                is SceneActivationResult.PartialSuccess ->
                    sendEvent(UiEvent.ShowSnackbar("Scene partially activated — ${result.failedDevices.size} device(s) didn't respond"))
                is SceneActivationResult.Failed ->
                    sendEvent(UiEvent.ShowSnackbar("Scene failed: ${result.reason}"))
            }
        }
    }

    private fun sampleScenes() = listOf<Scene>() // Replaced by DB in Phase 5
}
