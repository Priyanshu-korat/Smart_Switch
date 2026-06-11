package com.smarthome.core.repository

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Amplify
import com.smarthome.core.datastore.TokenManager
import com.smarthome.core.error.AppError
import com.smarthome.core.error.DomainResult
import com.smarthome.core.error.runDomainCatching
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class CognitoAuthRepository @Inject constructor(
    private val tokenManager: TokenManager
) : AuthRepository {

    private val _authState = MutableStateFlow(true) // Mocked to true

    init {
        // Mock session check
        _authState.value = true
    }

    override fun getAuthState(): Flow<Boolean> = _authState.asStateFlow()


    override suspend fun login(email: String, password: String): DomainResult<Unit> = runDomainCatching {
        // Mock successful login
        _authState.value = true
    }

    override suspend fun register(email: String, password: String, name: String): DomainResult<Unit> = runDomainCatching {
        // Mock successful registration
    }

    override suspend fun confirmSignUp(email: String, otp: String): DomainResult<Unit> = runDomainCatching {
        // Mock success
    }

    override suspend fun resendSignUpCode(email: String): DomainResult<Unit> = runDomainCatching {
        // Mock success
    }

    override suspend fun logout(): DomainResult<Unit> = runDomainCatching {
        GlobalScope.launch { tokenManager.clearTokens() }
        _authState.value = false
    }

    override suspend fun getAccessToken(): String? {
        return "mock_token"
    }

    private suspend fun fetchAndStoreTokens() {
        val accessToken = getAccessToken()
        if (accessToken != null) {
            tokenManager.saveTokens(accessToken, "")
        }
    }

    private fun mapAuthException(e: AuthException): AppError {
        return when {
            e.javaClass.simpleName == "NotAuthorizedException" -> AppError.AuthError.InvalidCredentials()
            e.javaClass.simpleName == "SessionExpiredException" -> AppError.AuthError.SessionExpired
            else -> AppError.Unknown(e)
        }
    }
}
