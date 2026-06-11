package com.smarthome.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import timber.log.Timber

class PendingCommandWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = coroutineScope {
        Timber.d("PendingCommandWorker: Starting offline command flush")

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        )
        val dao = entryPoint.pendingCommandDao()
        val publisher = entryPoint.mqttPublisher()

        // 1. Delete commands older than 2 minutes
        val expirationTime = System.currentTimeMillis() - (2 * 60 * 1000)
        dao.deleteExpiredCommands(expirationTime)

        // 2. Fetch remaining pending commands
        val pending = dao.getPendingCommands()
        if (pending.isEmpty()) {
            Timber.d("PendingCommandWorker: No valid pending commands")
            return@coroutineScope Result.success()
        }

        Timber.d("PendingCommandWorker: Found ${pending.size} pending commands")

        var allSuccess = true
        for (cmd in pending) {
            try {
                // Parse original target from JSON
                val json = JSONObject(cmd.payloadJson)
                val targetState = json.getInt("Data") == 1
                val switchIndex = json.getInt("Api_no")

                Timber.d("PendingCommandWorker: Retrying command for ${cmd.deviceId} -> switch $switchIndex")

                val result = publisher.sendSwitchCommand(
                    deviceId = cmd.deviceId,
                    switchIndex = switchIndex,
                    targetState = targetState,
                    timeoutMs = 5000, // Give it a bit more time on reconnect
                    isRetry = true
                )

                if (result is com.smarthome.core.error.DomainResult.Success) {
                    dao.deleteCommand(cmd.commandId)
                } else {
                    allSuccess = false
                    Timber.w("PendingCommandWorker: Failed to retry command ${cmd.commandId}")
                }
            } catch (e: Exception) {
                Timber.e(e, "PendingCommandWorker: Error parsing/sending command ${cmd.commandId}")
                dao.deleteCommand(cmd.commandId) // Delete if corrupted
            }
        }

        if (allSuccess) Result.success() else Result.retry()
    }
}
