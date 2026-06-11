package com.smarthome.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_commands")
data class PendingCommandEntity(
    @PrimaryKey val commandId: String, // UUID
    val deviceId: String,
    val payloadJson: String,
    val timestamp: Long,
    val retryCount: Int
)
