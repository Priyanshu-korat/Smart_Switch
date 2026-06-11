package com.smarthome.core.connectivity

import kotlinx.coroutines.flow.Flow

enum class NetworkState {
    Available, Unavailable, Losing, Lost
}

interface ConnectivityObserver {
    fun observe(): Flow<NetworkState>
}
