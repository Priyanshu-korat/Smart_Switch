package com.smarthome.core.domain.usecase.auth

import com.smarthome.core.error.DomainResult
import com.smarthome.core.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        return authRepository.logout()
    }
}
