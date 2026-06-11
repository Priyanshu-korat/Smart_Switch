package com.smarthome.core.model

data class Device(
    val id: String,
    val name: String,
    val isOnline: Boolean,
    val lastSeenAt: Long,
    val switches: List<SwitchState>
)

data class SwitchState(
    val index: Int,
    val name: String,
    val state: Boolean,
    val icon: String,
    val roomId: String?
)
