package com.smarthome.core.network

import com.smarthome.core.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intercepts every outgoing Retrofit request and adds:
 * Authorization: Bearer <Cognito Access Token>
 *
 * Uses [AuthRepository.getAccessToken] which silently refreshes via Amplify.
 * If the token is null (user is logged out), the request proceeds without auth
 * and the server will return 401.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authRepository: AuthRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authRepository.getAccessToken() }

        val request = if (token != null) {
            Timber.v("AuthInterceptor: Attaching Bearer token")
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            Timber.w("AuthInterceptor: No access token — proceeding unauthenticated")
            chain.request()
        }

        return chain.proceed(request)
    }
}
