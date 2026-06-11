package com.smarthome.core.repository

import com.smarthome.core.error.DomainResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAuthState(): Flow<Boolean>
    suspend fun login(email: String, password: String): DomainResult<Unit>
    suspend fun register(email: String, password: String, name: String): DomainResult<Unit>
    suspend fun confirmSignUp(email: String, otp: String): DomainResult<Unit>
    suspend fun resendSignUpCode(email: String): DomainResult<Unit>
    suspend fun logout(): DomainResult<Unit>
    suspend fun getAccessToken(): String?
}
