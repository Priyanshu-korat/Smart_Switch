package com.smarthome.core.model

data class Scene(
    val id: String,
    val name: String,
    val icon: String,
    val targets: List<SceneTarget>
)

data class SceneTarget(
    val deviceId: String,
    val switchIndex: Int,
    val targetState: Boolean
)
