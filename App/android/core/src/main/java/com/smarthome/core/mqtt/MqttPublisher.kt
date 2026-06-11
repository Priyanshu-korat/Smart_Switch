package com.smarthome.core.mqtt

import com.smarthome.core.constants.MqttConstants
import com.smarthome.core.constants.SceneConstants
import com.smarthome.core.error.AppError
import com.smarthome.core.error.DomainResult
import com.smarthome.core.model.SceneActivationResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smarthome.core.database.dao.PendingCommandDao
import com.smarthome.core.database.entity.PendingCommandEntity
import com.smarthome.core.work.SmartHomeWorkManager

data class SwitchCommand(
    val deviceId: String,
    val switchIndex: Int,
    val targetState: Boolean
)

@Singleton
class MqttPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mqttManager: MqttManager,
    private val commandTracker: CommandTracker,
    private val pendingCommandDao: PendingCommandDao,
    private val workManager: SmartHomeWorkManager
) {

    /**
     * Send a single switch command (ON/OFF) and await ACK from /state.
     * Uses CommandTracker to bridge firmware v1's state-based ACK.
     */
    suspend fun sendSwitchCommand(
        deviceId: String,
        switchIndex: Int,
        targetState: Boolean,
        timeoutMs: Long = 3000L,
        isRetry: Boolean = false
    ): DomainResult<Unit> {
        val ackKey = Triple(deviceId, switchIndex, targetState)
        val handle = CommandHandle(ackKey = ackKey)
        commandTracker.registerCommand(handle)

        val topic = "${MqttConstants.TOPIC_BASE}/$deviceId/${MqttConstants.TOPIC_COMMAND_SUFFIX}"
        val payload = JSONObject().apply {
            put("Req_type", 1)          // Command type per firmware spec
            put("Api_no", switchIndex)
            put("Data", if (targetState) 1 else 0)
        }.toString()

        val publishResult = mqttManager.publish(topic, payload)
        if (publishResult is DomainResult.Error) {
            commandTracker.cancel(handle)
            Timber.w("MqttPublisher: Publish failed, queuing command for offline retry")
            if (!isRetry) queueOfflineCommand(deviceId, payload)
            // We return a specific error so the UI knows it was queued
            return DomainResult.Error(AppError.NetworkError.NoConnection)
        }

        Timber.d("MqttPublisher: Command sent to $topic → switchIndex=$switchIndex, state=$targetState")
        val ackResult = commandTracker.awaitAck(handle, timeoutMs)
        
        if (ackResult is DomainResult.Error) {
            Timber.w("MqttPublisher: Command timed out, queuing for offline retry")
            if (!isRetry) queueOfflineCommand(deviceId, payload)
        }
        return ackResult
    }

    private suspend fun queueOfflineCommand(deviceId: String, payloadJson: String) {
        val entity = PendingCommandEntity(
            commandId = UUID.randomUUID().toString(),
            deviceId = deviceId,
            payloadJson = payloadJson,
            timestamp = System.currentTimeMillis(),
            retryCount = 0
        )
        pendingCommandDao.insertCommand(entity)
        workManager.enqueuePendingCommand(context)
    }

    /**
     * Activate a scene by firing ALL switch commands in parallel.
     * Returns PartialSuccess if any device fails, Success if all succeed.
     */
    suspend fun sendSceneCommands(
        commands: List<SwitchCommand>,
        timeoutMs: Long = SceneConstants.SCENE_CONFIRMATION_TIMEOUT_MS
    ): SceneActivationResult = coroutineScope {
        val results = commands.map { command ->
            async {
                command to sendSwitchCommand(
                    deviceId = command.deviceId,
                    switchIndex = command.switchIndex,
                    targetState = command.targetState,
                    timeoutMs = timeoutMs
                )
            }
        }.awaitAll()

        val failed = results.filter { (_, result) -> result is DomainResult.Error }

        when {
            failed.isEmpty() -> SceneActivationResult.Success
            failed.size == commands.size -> SceneActivationResult.Failed("All devices timed out")
            else -> SceneActivationResult.PartialSuccess(
                failedDevices = failed.map { (cmd, _) -> cmd.deviceId }
            )
        }
    }
}
