package com.smarthome.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String, // composite or simple ID
    val type: String, // "device", "scene"
    val targetId: String,
    val orderIndex: Int
)
