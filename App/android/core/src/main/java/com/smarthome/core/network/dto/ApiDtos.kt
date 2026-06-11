package com.smarthome.core.network.dto

import com.google.gson.annotations.SerializedName

// ── Device DTOs ──────────────────────────────────────────────

data class DeviceDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String,
    @SerializedName("lastSeenAt") val lastSeenAt: Long,
    @SerializedName("isOnline") val isOnline: Boolean
)

data class SwitchDto(
    @SerializedName("switchIndex") val switchIndex: Int,
    @SerializedName("state") val state: Boolean,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("roomId") val roomId: String?
)

data class ClaimDeviceRequest(
    @SerializedName("deviceId") val deviceId: String
)

data class UpdateSwitchesRequest(
    @SerializedName("switches") val switches: List<SwitchDto>
)

// ── Scene DTOs ──────────────────────────────────────────────

data class SceneDto(
    @SerializedName("sceneId") val sceneId: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("targets") val targets: List<SceneTargetDto>
)

data class SceneTargetDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("switchIndex") val switchIndex: Int,
    @SerializedName("targetState") val targetState: Boolean
)

data class CreateSceneRequest(
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("targets") val targets: List<SceneTargetDto>
)

// ── Schedule DTOs ──────────────────────────────────────────

data class ScheduleDto(
    @SerializedName("scheduleId") val scheduleId: String,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("switchIndex") val switchIndex: Int,
    @SerializedName("targetState") val targetState: Boolean,
    @SerializedName("cronExpression") val cronExpression: String,
    @SerializedName("label") val label: String,
    @SerializedName("isEnabled") val isEnabled: Boolean
)

data class CreateScheduleRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("switchIndex") val switchIndex: Int,
    @SerializedName("targetState") val targetState: Boolean,
    @SerializedName("cronExpression") val cronExpression: String,
    @SerializedName("label") val label: String
)

// ── Notification DTOs ──────────────────────────────────────

data class NotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("category") val category: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("isRead") val isRead: Boolean
)

// ── Room DTOs ──────────────────────────────────────────────

data class RoomDto(
    @SerializedName("roomId") val roomId: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String
)

// ── User DTOs ─────────────────────────────────────────────

data class UpdateFcmTokenRequest(
    @SerializedName("fcmToken") val fcmToken: String
)

// ── Generic Wrapper ────────────────────────────────────────

data class ApiResponse<T>(
    @SerializedName("data") val data: T?,
    @SerializedName("message") val message: String?
)

data class ListResponse<T>(
    @SerializedName("items") val items: List<T>
)
