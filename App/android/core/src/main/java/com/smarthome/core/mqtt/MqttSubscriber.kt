package com.smarthome.core.mqtt

import com.smarthome.core.constants.MqttConstants
import com.smarthome.core.database.dao.DeviceDao
import com.smarthome.core.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses all incoming MQTT messages from MqttManager's global listener.
 * Firmware sends /state messages with:
 *  - Req_type: 1 = command ack, 2 = physical press
 *  - Api_no:  switch index (0-3)
 *  - Data:    relay state array [1,0,0,0]
 *  - deviceId is extracted from the topic path: smarthome/{deviceId}/state
 */
@Singleton
class MqttSubscriber @Inject constructor(
    private val commandTracker: CommandTracker,
    private val deviceDao: DeviceDao,
    private val timeProvider: TimeProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun attach(mqttManager: MqttManager) {
        mqttManager.setMessageCallback { topic, payload ->
            handleMessage(topic, payload)
        }
    }

    private fun handleMessage(topic: String, payload: ByteArray) {
        val deviceId = extractDeviceId(topic) ?: return
        val message = String(payload)

        try {
            val json = JSONObject(message)
            val reqType = json.optInt("Req_type", -1)
            val switchIndex = json.optInt("Api_no", -1)

            val dataArray = json.optJSONArray("Data") ?: return
            if (switchIndex < 0 || switchIndex >= dataArray.length()) return

            val state = dataArray.getInt(switchIndex) == 1

            Timber.v("MqttSubscriber: [$deviceId] Req_type=$reqType, switch=$switchIndex, state=$state")

            // 1. Update Room DB state optimistically regardless of Req_type
            scope.launch {
                deviceDao.updateSwitchState(deviceId, switchIndex, state)
                deviceDao.observeDevice(deviceId).collect { device ->
                    device?.let {
                        deviceDao.insertOrUpdateDevice(
                            it.copy(lastSeenAt = timeProvider.now(), isOnline = true)
                        )
                    }
                    return@collect
                }
            }

            // 2. Notify CommandTracker — resolves CompletableDeferred for Req_type 1 ACK
            commandTracker.onStateReceived(deviceId, switchIndex, state)

        } catch (e: JSONException) {
            Timber.e("MqttSubscriber: Failed to parse message from $deviceId: ${e.message}")
        }
    }

    private fun extractDeviceId(topic: String): String? {
        // Pattern: smarthome/{deviceId}/state
        val parts = topic.split("/")
        return if (parts.size == 3 && parts[0] == MqttConstants.TOPIC_BASE && parts[2] == MqttConstants.TOPIC_STATE_SUFFIX) {
            parts[1]
        } else null
    }
}
