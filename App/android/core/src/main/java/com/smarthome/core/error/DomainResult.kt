package com.smarthome.core.error

sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Error(val error: AppError) : DomainResult<Nothing>
}

// Extension to map common results
inline fun <T> runDomainCatching(block: () -> T): DomainResult<T> {
    return try {
        DomainResult.Success(block())
    } catch (e: AppError) {
        DomainResult.Error(e)
    } catch (e: Exception) {
        DomainResult.Error(AppError.Unknown(e))
    }
}
