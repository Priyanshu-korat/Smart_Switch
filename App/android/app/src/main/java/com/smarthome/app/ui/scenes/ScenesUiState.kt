package com.smarthome.app.ui.scenes

import com.smarthome.core.model.Scene
import com.smarthome.core.model.SceneActivationResult

data class ScenesUiState(
    val isLoading: Boolean = true,
    val scenes: List<Scene> = emptyList(),
    val activatingSceneId: String? = null,
    val lastResult: SceneActivationResult? = null
)
