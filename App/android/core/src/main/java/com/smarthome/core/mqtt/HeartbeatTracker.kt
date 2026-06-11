package com.smarthome.core.mqtt

import com.smarthome.core.constants.MqttConstants
import com.smarthome.core.database.dao.DeviceDao
import com.smarthome.core.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodically checks the database for devices that haven't been seen recently.
 * Marks them as offline if their lastSeenAt timestamp is older than the timeout.
 */
@Singleton
class HeartbeatTracker @Inject constructor(
    private val deviceDao: DeviceDao,
    private val timeProvider: TimeProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var trackingJob: Job? = null

    fun startTracking() {
        if (trackingJob?.isActive == true) return
        
        Timber.d("HeartbeatTracker: Starting offline detection loop")
        trackingJob = scope.launch {
            while (isActive) {
                try {
                    val cutoffTime = timeProvider.now() - MqttConstants.HEARTBEAT_TIMEOUT_MS
                    deviceDao.markStaleDevicesOffline(cutoffTime)
                } catch (e: Exception) {
                    Timber.e(e, "HeartbeatTracker: Failed to mark stale devices offline")
                }
                // Check every 10 seconds
                delay(10_000L)
            }
        }
    }

    fun stopTracking() {
        Timber.d("HeartbeatTracker: Stopping offline detection loop")
        trackingJob?.cancel()
        trackingJob = null
    }
}
