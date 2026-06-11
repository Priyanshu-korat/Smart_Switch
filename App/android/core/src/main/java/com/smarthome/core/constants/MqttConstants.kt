package com.smarthome.core.constants

object MqttConstants {
    const val TOPIC_BASE = "smarthome"
    const val TOPIC_STATE_SUFFIX = "state"
    const val TOPIC_COMMAND_SUFFIX = "command"
    const val QUALITY_OF_SERVICE = 1
    const val HEARTBEAT_TIMEOUT_MS = 90_000L // 90 seconds
    const val RECONNECT_DELAY_BASE_MS = 1000L
    const val RECONNECT_DELAY_MAX_MS = 30000L
}
