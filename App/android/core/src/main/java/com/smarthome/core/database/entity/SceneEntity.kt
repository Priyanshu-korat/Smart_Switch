package com.smarthome.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val targetsJson: String // Serialized List<SceneTarget>
)
