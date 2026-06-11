package com.smarthome.core.time

import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun now(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
