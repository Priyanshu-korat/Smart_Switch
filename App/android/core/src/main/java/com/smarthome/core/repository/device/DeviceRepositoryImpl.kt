package com.smarthome.core.repository.device

import com.smarthome.core.database.dao.DeviceDao
import com.smarthome.core.database.entity.DeviceEntity
import com.smarthome.core.database.entity.DeviceStateEntity
import com.smarthome.core.database.relation.DeviceWithStates
import com.smarthome.core.error.DomainResult
import com.smarthome.core.model.Device
import com.smarthome.core.model.SwitchState
import com.smarthome.core.mqtt.MqttPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
    private val mqttPublisher: MqttPublisher
) : DeviceRepository {

    init {
        // Pre-populate database with mock data if it's empty to allow previewing the UI
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val devices = deviceDao.observeAllDevicesWithStates().first()
                if (devices.isEmpty()) {
                    Timber.d("DeviceRepositoryImpl: Database is empty. Pre-populating mock devices for preview.")
                    val now = System.currentTimeMillis()
                    val dev1 = DeviceEntity("dev_1", "Living Room Hub", now - 1000, true, true, "owner")
                    val states1 = listOf(
                        DeviceStateEntity("dev_1", 0, true, "Main Light", "lightbulb", "room_1"),
                        DeviceStateEntity("dev_1", 1, false, "TV Power", "tv", "room_1")
                    )
                    
                    val dev2 = DeviceEntity("dev_2", "Kitchen Panel", now - 5000, true, true, "owner")
                    val states2 = listOf(
                        DeviceStateEntity("dev_2", 0, true, "Overhead", "lightbulb", "room_2"),
                        DeviceStateEntity("dev_2", 1, true, "Coffee Maker", "coffee", "room_2"),
                        DeviceStateEntity("dev_2", 2, false, "Counter Lights", "lightbulb", "room_2")
                    )

                    val dev3 = DeviceEntity("dev_3", "Bedroom Lamp", now - 3600000, false, true, "owner")
                    val states3 = listOf(
                        DeviceStateEntity("dev_3", 0, false, "Lamp", "lightbulb", "room_3")
                    )

                    deviceDao.insertDeviceWithStates(dev1, states1)
                    deviceDao.insertDeviceWithStates(dev2, states2)
                    deviceDao.insertDeviceWithStates(dev3, states3)
                }
            } catch (e: Exception) {
                Timber.e(e, "DeviceRepositoryImpl: Failed to pre-populate mock data")
            }
        }
    }

    override fun observeDevices(): Flow<List<Device>> {
        return deviceDao.observeAllDevicesWithStates().map { relations ->
            relations.map { it.toDomainModel() }
        }
    }

    override fun observeDevice(deviceId: String): Flow<Device?> {
        return deviceDao.observeDeviceWithStates(deviceId).map { it?.toDomainModel() }
    }

    override suspend fun toggleSwitch(
        deviceId: String,
        switchIndex: Int,
        targetState: Boolean
    ): DomainResult<Unit> {
        // Optimistic update — update local DB immediately so UI feels instant
        deviceDao.updateSwitchState(deviceId, switchIndex, targetState)

        Timber.d("DeviceRepository: Toggling switch $switchIndex on $deviceId → $targetState")

        // Fire MQTT command and await ACK. If timeout, the state stays as toggled
        // (will self-correct on next /state message from device)
        return mqttPublisher.sendSwitchCommand(deviceId, switchIndex, targetState)
    }

    override suspend fun syncDevices(): DomainResult<Unit> {
        // Sync from API will be implemented in Phase 5 with Retrofit/API layer
        return DomainResult.Success(Unit)
    }

    override suspend fun removeDevice(deviceId: String): DomainResult<Unit> {
        return try {
            deviceDao.deleteDevice(deviceId)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(com.smarthome.core.error.AppError.Unknown(e))
        }
    }

    private fun DeviceWithStates.toDomainModel(): Device {
        return Device(
            id = device.id,
            name = device.name,
            isOnline = device.isOnline,
            lastSeenAt = device.lastSeenAt,
            switches = states.map { s -> 
                SwitchState(
                    index = s.switchIndex,
                    name = s.name,
                    state = s.state,
                    icon = s.icon,
                    roomId = s.roomId
                ) 
            }.sortedBy { it.index }
        )
    }
}
