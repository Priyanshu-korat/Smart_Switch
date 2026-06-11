package com.smarthome.core.domain.usecase.auth

import com.smarthome.core.error.DomainResult
import com.smarthome.core.repository.AuthRepository
import javax.inject.Inject

class ConfirmSignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, otp: String): DomainResult<Unit> {
        return authRepository.confirmSignUp(email, otp)
    }
}
