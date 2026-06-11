package com.smarthome.core.provisioning

import com.smarthome.core.constants.ProvisionConstants
import com.smarthome.core.error.AppError
import com.smarthome.core.error.DomainResult
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ProvisionRequest(
    val ssid: String,
    val password: String,
    val mqttHost: String,
    val mqttPort: Int,
    val mqttUser: String,
    val mqttPass: String
)

@Singleton
class SoftApClient @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(ProvisionConstants.PROVISION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(ProvisionConstants.PROVISION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(ProvisionConstants.PROVISION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Sends Wi-Fi + MQTT credentials to the device's SoftAP provisioning endpoint.
     * Endpoint: POST http://192.168.4.1/provision
     */
    suspend fun provisionDevice(request: ProvisionRequest): DomainResult<String> {
        return try {
            val baseUrl = "http://${ProvisionConstants.SOFTAP_IP}:${ProvisionConstants.HTTP_PORT}"

            val body = FormBody.Builder()
                .add("ssid", request.ssid)
                .add("password", request.password)
                .add("mqtt_host", request.mqttHost)
                .add("mqtt_port", request.mqttPort.toString())
                .add("mqtt_user", request.mqttUser)
                .add("mqtt_pass", request.mqttPass)
                .build()

            val httpRequest = Request.Builder()
                .url("$baseUrl/provision")
                .post(body)
                .build()

            Timber.d("SoftApClient: Sending provision request to $baseUrl/provision")

            val response = httpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return DomainResult.Error(AppError.ProvisioningError.WrongPassword)
            }

            // Parse deviceId from response JSON
            val json = JSONObject(responseBody)
            val deviceId = json.optString("device_id", "")

            if (deviceId.isBlank()) {
                return DomainResult.Error(AppError.Unknown())
            }

            Timber.d("SoftApClient: Provisioning successful. DeviceId: $deviceId")
            DomainResult.Success(deviceId)

        } catch (e: IOException) {
            Timber.e("SoftApClient: Network error during provisioning: ${e.message}")
            DomainResult.Error(AppError.ProvisioningError.ApNotFound)
        } catch (e: Exception) {
            Timber.e("SoftApClient: Unexpected error: ${e.message}")
            DomainResult.Error(AppError.Unknown(e))
        }
    }

    /**
     * Checks if the device is reachable at 192.168.4.1 (device in SoftAP mode).
     */
    fun isDeviceReachable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("http://${ProvisionConstants.SOFTAP_IP}/ping")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
