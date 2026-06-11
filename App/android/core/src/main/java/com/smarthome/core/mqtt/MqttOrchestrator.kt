package com.smarthome.core.mqtt

import com.smarthome.core.constants.MqttConstants
import com.smarthome.core.error.DomainResult
import com.smarthome.core.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full MQTT lifecycle:
 * 1. Listens for auth state changes
 * 2. On login: connects to HiveMQ, subscribes to all owned device topics
 * 3. On logout: disconnects cleanly
 * 4. Attaches MqttSubscriber to MqttManager's message stream
 */
@Singleton
class MqttOrchestrator @Inject constructor(
    private val mqttManager: MqttManager,
    private val mqttSubscriber: MqttSubscriber,
    private val authRepository: AuthRepository,
    private val heartbeatTracker: HeartbeatTracker
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val IOT_CORE_HOST = "a3hacg7yvwprwz-ats.iot.ap-south-1.amazonaws.com"
        private const val IOT_CORE_PORT = 8883
    }

    fun initialize() {
        // Attach subscriber to manager's message stream
        mqttSubscriber.attach(mqttManager)

        // Monitor auth state — connect on login, disconnect on logout
        scope.launch {
            authRepository.getAuthState().collectLatest { isLoggedIn ->
                if (isLoggedIn) {
                    connectAndSubscribe()
                    heartbeatTracker.startTracking()
                } else {
                    heartbeatTracker.stopTracking()
                    mqttManager.disconnect()
                }
            }
        }
    }

    private suspend fun connectAndSubscribe() {
        val accessToken = authRepository.getAccessToken() ?: return

        Timber.d("MqttOrchestrator: Connecting to IoT Core...")
        val result = mqttManager.connect(
            host = IOT_CORE_HOST,
            port = IOT_CORE_PORT,
            clientId = accessToken,
            username = accessToken,
            password = accessToken 
        )

        when (result) {
            is DomainResult.Success -> {
                Timber.d("MqttOrchestrator: Connected. Subscribing to device topics...")
                // Subscribe to wildcard for user-owned devices
                // In production, enumerate per-device topics from DeviceDao
                mqttManager.subscribe("${MqttConstants.TOPIC_BASE}/+/${MqttConstants.TOPIC_STATE_SUFFIX}")
            }
            is DomainResult.Error -> {
                Timber.e("MqttOrchestrator: Connection failed: ${result.error.userMessage}")
            }
        }
    }
}
