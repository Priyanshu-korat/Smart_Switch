package com.smarthome.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.smarthome.core.database.entity.DeviceEntity
import com.smarthome.core.database.entity.DeviceStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun observeAllDevices(): Flow<List<DeviceEntity>>

    @Transaction
    @Query("SELECT * FROM devices")
    fun observeAllDevicesWithStates(): Flow<List<com.smarthome.core.database.relation.DeviceWithStates>>

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    fun observeDevice(deviceId: String): Flow<DeviceEntity?>

    @Transaction
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    fun observeDeviceWithStates(deviceId: String): Flow<com.smarthome.core.database.relation.DeviceWithStates?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDeviceStates(states: List<DeviceStateEntity>)

    @Query("SELECT * FROM device_states WHERE deviceId = :deviceId")
    fun observeDeviceStates(deviceId: String): Flow<List<DeviceStateEntity>>

    @Transaction
    suspend fun insertDeviceWithStates(device: DeviceEntity, states: List<DeviceStateEntity>) {
        insertOrUpdateDevice(device)
        insertOrUpdateDeviceStates(states)
    }

    @Query("UPDATE device_states SET state = :state WHERE deviceId = :deviceId AND switchIndex = :switchIndex")
    suspend fun updateSwitchState(deviceId: String, switchIndex: Int, state: Boolean)

    @Query("UPDATE devices SET isOnline = 0 WHERE lastSeenAt < :cutoffTime AND isOnline = 1")
    suspend fun markStaleDevicesOffline(cutoffTime: Long)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: String)
}
