package com.smarthome.core.domain.usecase.auth

import com.smarthome.core.error.DomainResult
import com.smarthome.core.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): DomainResult<Unit> {
        return authRepository.login(email, password)
    }
}
