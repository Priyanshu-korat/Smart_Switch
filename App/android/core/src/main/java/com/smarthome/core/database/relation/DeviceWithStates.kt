package com.smarthome.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.smarthome.core.database.entity.DeviceEntity
import com.smarthome.core.database.entity.DeviceStateEntity

data class DeviceWithStates(
    @Embedded val device: DeviceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "deviceId"
    )
    val states: List<DeviceStateEntity>
)
