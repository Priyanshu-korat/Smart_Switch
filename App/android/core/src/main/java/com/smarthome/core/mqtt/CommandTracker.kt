package com.smarthome.core.mqtt

import com.smarthome.core.error.AppError
import com.smarthome.core.error.DomainResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the gap between the async MQTT publish and the incoming /state ACK.
 *
 * Firmware v1 does NOT include a commandId in its ACK payload. So we use a
 * Triple(deviceId, switchIndex, targetState) as the ackKey — matching the
 * exact switch state change we expect to see in the /state response.
 *
 * Firmware v2 will include a UUID commandId in every ACK, removing the need
 * for ackKey entirely.
 */
data class CommandHandle(
    val commandId: String = UUID.randomUUID().toString(),
    val ackKey: Triple<String, Int, Boolean>, // (deviceId, switchIndex, targetState) - Firmware v1 bridge
    internal val deferred: CompletableDeferred<Unit> = CompletableDeferred()
)

@Singleton
class CommandTracker @Inject constructor() {

    // Primary map by commandId - for v2 firmware
    private val pendingById = ConcurrentHashMap<String, CommandHandle>()

    // Secondary map by ackKey - for v1 firmware state-matching
    private val ackBridge = ConcurrentHashMap<Triple<String, Int, Boolean>, String>() // ackKey -> commandId

    fun registerCommand(handle: CommandHandle) {
        pendingById[handle.commandId] = handle
        ackBridge[handle.ackKey] = handle.commandId
        Timber.d("CommandTracker: Registered command ${handle.commandId} for ack key ${handle.ackKey}")
    }

    /**
     * Called by MqttSubscriber when a /state message arrives (Firmware v1 ACK).
     * Matches by (deviceId, switchIndex, targetState).
     */
    fun onStateReceived(deviceId: String, switchIndex: Int, state: Boolean) {
        val ackKey = Triple(deviceId, switchIndex, state)
        val commandId = ackBridge.remove(ackKey) ?: return
        val handle = pendingById.remove(commandId) ?: return
        Timber.d("CommandTracker: ACK received for command $commandId via ackKey")
        handle.deferred.complete(Unit)
    }

    /**
     * O(1) cleanup using the stored ackKey — no reverse lookup needed.
     */
    fun cancel(handle: CommandHandle) {
        pendingById.remove(handle.commandId)
        ackBridge.remove(handle.ackKey)
        handle.deferred.cancel()
    }

    suspend fun awaitAck(
        handle: CommandHandle,
        timeoutMs: Long
    ): DomainResult<Unit> {
        return try {
            val result = withTimeoutOrNull(timeoutMs) {
                handle.deferred.await()
            }
            if (result == null) {
                cancel(handle)
                DomainResult.Error(AppError.MqttError.CommandTimeout)
            } else {
                DomainResult.Success(Unit)
            }
        } catch (e: Exception) {
            cancel(handle)
            DomainResult.Error(AppError.Unknown(e))
        }
    }
}
