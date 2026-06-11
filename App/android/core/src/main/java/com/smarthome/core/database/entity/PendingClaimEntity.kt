package com.smarthome.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_claims")
data class PendingClaimEntity(
    @PrimaryKey val deviceId: String,
    val timestamp: Long
)
