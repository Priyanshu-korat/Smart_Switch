package com.smarthome.core.mqtt

import com.smarthome.core.constants.MqttConstants
import com.smarthome.core.error.AppError
import com.smarthome.core.error.DomainResult
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

enum class MqttConnectionState { Disconnected, Connecting, Connected, Reconnecting }

@Singleton
class MqttManager @Inject constructor() {

    private var client: Mqtt3AsyncClient? = null
    private val _connectionState = MutableStateFlow(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private var onMessageCallback: ((String, ByteArray) -> Unit)? = null
    private val isConnecting = AtomicBoolean(false)

    fun setMessageCallback(callback: (topic: String, payload: ByteArray) -> Unit) {
        onMessageCallback = callback
    }

    suspend fun connect(
        host: String,
        port: Int,
        clientId: String,
        username: String,
        password: String
    ): DomainResult<Unit> {
        if (isConnecting.getAndSet(true)) {
            return DomainResult.Error(AppError.MqttError.ConnectionLost)
        }

        _connectionState.value = MqttConnectionState.Connecting

        return try {
            val newClient = Mqtt3Client.builder()
                .identifier("android-${clientId.take(8)}-${UUID.randomUUID().toString().take(8)}")
                .serverHost(host)
                .serverPort(port)
                .simpleAuth()
                .username(username)
                .password(password.toByteArray())
                .applySimpleAuth()
                .automaticReconnect()
                .initialDelay(MqttConstants.RECONNECT_DELAY_BASE_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .maxDelay(MqttConstants.RECONNECT_DELAY_MAX_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .applyAutomaticReconnect()
                .addConnectedListener {
                    Timber.d("MqttManager: Connected")
                    _connectionState.value = MqttConnectionState.Connected
                }
                .addDisconnectedListener { ctx ->
                    val isReconnecting = ctx.reconnector.isReconnect
                    _connectionState.value = if (isReconnecting) {
                        MqttConnectionState.Reconnecting
                    } else {
                        MqttConnectionState.Disconnected
                    }
                    Timber.w("MqttManager: Disconnected. Reconnecting: $isReconnecting. Cause: ${ctx.cause?.message}")
                }
                .buildAsync()

            suspendCoroutine { continuation ->
                newClient.connect()
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            isConnecting.set(false)
                            continuation.resumeWithException(throwable)
                        } else {
                            client = newClient
                            isConnecting.set(false)
                            setupGlobalListener(newClient)
                            continuation.resume(Unit)
                        }
                    }
            }

            DomainResult.Success(Unit)
        } catch (e: Exception) {
            _connectionState.value = MqttConnectionState.Disconnected
            isConnecting.set(false)
            DomainResult.Error(AppError.MqttError.ConnectionLost)
        }
    }

    private fun setupGlobalListener(mqttClient: Mqtt3AsyncClient) {
        mqttClient.publishes(MqttGlobalPublishFilter.ALL) { publish ->
            val topic = publish.topic.toString()
            val payload = publish.payloadAsBytes
            Timber.v("MqttManager: Received on '$topic': ${String(payload)}")
            onMessageCallback?.invoke(topic, payload)
        }
    }

    suspend fun subscribe(topic: String): DomainResult<Unit> {
        val c = client ?: return DomainResult.Error(AppError.MqttError.ConnectionLost)
        return try {
            suspendCoroutine { continuation ->
                c.subscribeWith()
                    .topicFilter(topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send()
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            continuation.resumeWithException(throwable)
                        } else {
                            Timber.d("MqttManager: Subscribed to '$topic'")
                            continuation.resume(Unit)
                        }
                    }
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(AppError.MqttError.ConnectionLost)
        }
    }

    suspend fun publish(topic: String, payload: String): DomainResult<Unit> {
        val c = client ?: return DomainResult.Error(AppError.MqttError.ConnectionLost)
        return try {
            suspendCoroutine { continuation ->
                c.publishWith()
                    .topic(topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .payload(payload.toByteArray())
                    .send()
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            continuation.resumeWithException(throwable)
                        } else {
                            continuation.resume(Unit)
                        }
                    }
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(AppError.MqttError.ConnectionLost)
        }
    }

    fun disconnect() {
        client?.disconnect()
        client = null
        _connectionState.value = MqttConnectionState.Disconnected
        Timber.d("MqttManager: Disconnected")
    }
}
