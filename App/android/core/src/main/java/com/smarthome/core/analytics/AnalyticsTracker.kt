package com.smarthome.core.analytics

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

interface AnalyticsTracker {
    fun trackEvent(eventName: String, params: Map<String, Any> = emptyMap())
}

@Singleton
class TimberAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun trackEvent(eventName: String, params: Map<String, Any>) {
        Timber.tag("Analytics").d("Event: %s | Params: %s", eventName, params)
    }
}
