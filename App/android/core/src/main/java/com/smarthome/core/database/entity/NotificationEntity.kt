package com.smarthome.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val category: String, // "alert", "system", "device"
    val title: String,
    val message: String,
    val deviceId: String?,
    val isRead: Boolean
)
