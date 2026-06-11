package com.smarthome.core.provisioning

import com.smarthome.core.database.dao.DeviceDao
import com.smarthome.core.database.entity.DeviceEntity
import com.smarthome.core.error.DomainResult
import com.smarthome.core.time.TimeProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface ProvisioningRepository {
    suspend fun provisionDevice(
        ssid: String,
        wifiPassword: String,
        mqttHost: String,
        mqttPort: Int,
        mqttUser: String,
        mqttPass: String
    ): DomainResult<String>
}

@Singleton
class ProvisioningRepositoryImpl @Inject constructor(
    private val softApClient: SoftApClient,
    private val deviceDao: DeviceDao,
    private val timeProvider: TimeProvider
) : ProvisioningRepository {

    override suspend fun provisionDevice(
        ssid: String,
        wifiPassword: String,
        mqttHost: String,
        mqttPort: Int,
        mqttUser: String,
        mqttPass: String
    ): DomainResult<String> {
        val request = ProvisionRequest(
            ssid = ssid,
            password = wifiPassword,
            mqttHost = mqttHost,
            mqttPort = mqttPort,
            mqttUser = mqttUser,
            mqttPass = mqttPass
        )

        val result = softApClient.provisionDevice(request)

        if (result is DomainResult.Success) {
            val deviceId = result.data
            Timber.d("ProvisioningRepository: Saving device $deviceId to local DB")

            // Save as pending_claim — will be confirmed after MQTT heartbeat arrives
            deviceDao.insertOrUpdateDevice(
                DeviceEntity(
                    id = deviceId,
                    name = "New Device",
                    lastSeenAt = timeProvider.now(),
                    isOnline = false,
                    isClaimed = false,
                    role = "owner"
                )
            )
        }

        return result
    }
}
