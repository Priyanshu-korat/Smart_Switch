package com.smarthome.core.analytics

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

interface CrashReporter {
    fun recordException(e: Throwable)
    fun setUserId(userId: String)
}

@Singleton
class TimberCrashReporter @Inject constructor() : CrashReporter {
    override fun recordException(e: Throwable) {
        Timber.tag("CrashReporter").e(e)
    }
    
    override fun setUserId(userId: String) {
        Timber.tag("CrashReporter").d("User ID set: %s", userId)
    }
}
