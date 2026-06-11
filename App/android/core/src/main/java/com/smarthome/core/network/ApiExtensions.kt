package com.smarthome.core.network

import com.google.gson.Gson
import com.smarthome.core.error.AppError
import com.smarthome.core.error.DomainResult
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber

/**
 * Wraps a Retrofit [Response<T>] into a [DomainResult<T>].
 * Handles HTTP error codes → appropriate [AppError] subtypes.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): DomainResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                DomainResult.Success(body)
            } else {
                // 204 No Content responses
                @Suppress("UNCHECKED_CAST")
                DomainResult.Success(Unit as T)
            }
        } else {
            val errorMessage = response.errorBody()?.string() ?: "Unknown server error"
            Timber.e("safeApiCall: HTTP ${response.code()} — $errorMessage")
            when (response.code()) {
                401 -> DomainResult.Error(AppError.AuthError.SessionExpired)
                403 -> DomainResult.Error(AppError.AuthError.InvalidCredentials())
                else -> DomainResult.Error(AppError.Unknown(Exception("HTTP ${response.code()}: $errorMessage")))
            }
        }
    } catch (e: java.net.UnknownHostException) {
        DomainResult.Error(AppError.NetworkError.NoConnection)
    } catch (e: java.net.SocketTimeoutException) {
        DomainResult.Error(AppError.NetworkError.Timeout)
    } catch (e: Exception) {
        Timber.e(e, "safeApiCall: Unexpected error")
        DomainResult.Error(AppError.Unknown(e))
    }
}
