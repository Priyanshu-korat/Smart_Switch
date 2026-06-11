package com.smarthome.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lastSeenAt: Long,
    val isOnline: Boolean,
    val isClaimed: Boolean,
    val role: String // "owner", "guest"
)
