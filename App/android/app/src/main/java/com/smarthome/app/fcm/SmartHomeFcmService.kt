package com.smarthome.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smarthome.app.MainActivity
import com.smarthome.core.database.dao.NotificationDao
import com.smarthome.core.database.entity.NotificationEntity
import com.smarthome.core.network.ApiService
import com.smarthome.core.network.dto.UpdateFcmTokenRequest
import com.smarthome.core.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SmartHomeFcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationDao: NotificationDao
    @Inject lateinit var apiService: ApiService
    @Inject lateinit var timeProvider: TimeProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "smarthome_alerts"
        const val CHANNEL_NAME = "SmartHome Alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Called when a new FCM token is generated.
     * We immediately register it with our backend so notifications reach this device.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM: New token generated")
        serviceScope.launch {
            try {
                apiService.updateFcmToken(UpdateFcmTokenRequest(token))
                Timber.d("FCM: Token registered with backend")
            } catch (e: Exception) {
                Timber.e(e, "FCM: Failed to register token")
            }
        }
    }

    /**
     * Handles incoming FCM push (from PhysicalPressFn Lambda via IoT Rule).
     * Payload structure:
     * {
     *   "title": "Switch Activated",
     *   "message": "Living Room Fan was turned ON",
     *   "deviceId": "SH-12345",
     *   "category": "device"
     * }
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM: Message received from ${message.from}")

        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "SmartHome"
        val body = data["message"] ?: message.notification?.body ?: ""
        val deviceId = data["deviceId"]
        val category = data["category"] ?: "device"

        // Persist notification in Room DB
        serviceScope.launch {
            notificationDao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = timeProvider.now(),
                    category = category,
                    title = title,
                    message = body,
                    deviceId = deviceId,
                    isRead = false
                )
            )
        }

        // Show system notification
        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Physical switch press alerts and scene activations"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
