package com.smarthome.core.network

import com.smarthome.core.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Devices ──────────────────────────────────────────────────────
    @GET("devices")
    suspend fun getDevices(): Response<ListResponse<DeviceDto>>

    @POST("devices/claim")
    suspend fun claimDevice(@Body request: ClaimDeviceRequest): Response<DeviceDto>

    @DELETE("devices/{id}")
    suspend fun removeDevice(@Path("id") deviceId: String): Response<Unit>

    @PUT("devices/{id}/switches")
    suspend fun updateSwitches(
        @Path("id") deviceId: String,
        @Body request: UpdateSwitchesRequest
    ): Response<Unit>

    // ── Rooms ────────────────────────────────────────────────────────
    @GET("rooms")
    suspend fun getRooms(): Response<ListResponse<RoomDto>>

    @POST("rooms")
    suspend fun createRoom(@Body room: RoomDto): Response<RoomDto>

    @PUT("rooms")
    suspend fun updateRoom(@Body room: RoomDto): Response<RoomDto>

    @DELETE("rooms")
    suspend fun deleteRoom(@Query("roomId") roomId: String): Response<Unit>

    // ── Scenes ───────────────────────────────────────────────────────
    @GET("scenes")
    suspend fun getScenes(): Response<ListResponse<SceneDto>>

    @POST("scenes")
    suspend fun createScene(@Body request: CreateSceneRequest): Response<SceneDto>

    @PUT("scenes")
    suspend fun updateScene(@Body scene: SceneDto): Response<SceneDto>

    @DELETE("scenes")
    suspend fun deleteScene(@Query("sceneId") sceneId: String): Response<Unit>

    // ── Schedules ────────────────────────────────────────────────────
    @GET("schedules")
    suspend fun getSchedules(): Response<ListResponse<ScheduleDto>>

    @POST("schedules")
    suspend fun createSchedule(@Body request: CreateScheduleRequest): Response<ScheduleDto>

    @PUT("schedules")
    suspend fun updateSchedule(@Body schedule: ScheduleDto): Response<ScheduleDto>

    @DELETE("schedules")
    suspend fun deleteSchedule(@Query("scheduleId") scheduleId: String): Response<Unit>

    // ── Notifications ────────────────────────────────────────────────
    @GET("notifications")
    suspend fun getNotifications(): Response<ListResponse<NotificationDto>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") notificationId: String): Response<Unit>

    // ── User ─────────────────────────────────────────────────────────
    @GET("users/me")
    suspend fun getProfile(): Response<ApiResponse<Map<String, Any>>>

    @PUT("users/fcm-token")
    suspend fun updateFcmToken(@Body request: UpdateFcmTokenRequest): Response<Unit>
}
