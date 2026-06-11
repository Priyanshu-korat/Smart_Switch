package com.smarthome.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "device_states",
    primaryKeys = ["deviceId", "switchIndex"],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deviceId")]
)
data class DeviceStateEntity(
    val deviceId: String,
    val switchIndex: Int,
    val state: Boolean,
    val name: String,
    val icon: String,
    val roomId: String?
)
