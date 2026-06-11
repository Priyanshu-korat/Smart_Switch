package com.smarthome.core.di

import com.smarthome.core.analytics.AnalyticsTracker
import com.smarthome.core.analytics.CrashReporter
import com.smarthome.core.analytics.TimberAnalyticsTracker
import com.smarthome.core.analytics.TimberCrashReporter
import com.smarthome.core.time.SystemTimeProvider
import com.smarthome.core.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    abstract fun bindAnalyticsTracker(
        timberAnalyticsTracker: TimberAnalyticsTracker
    ): AnalyticsTracker

    @Binds
    abstract fun bindCrashReporter(
        timberCrashReporter: TimberCrashReporter
    ): CrashReporter

    @Binds
    abstract fun bindTimeProvider(
        systemTimeProvider: SystemTimeProvider
    ): TimeProvider
}
