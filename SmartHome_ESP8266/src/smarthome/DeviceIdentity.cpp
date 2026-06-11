// =============================================================================
// DeviceIdentity.cpp
// =============================================================================

#include "DeviceIdentity.h"
#include "config.h"
#include <ESP8266WiFi.h>

// Singleton definition
DeviceIdentity DeviceID;

void DeviceIdentity::begin() {
    // Read MAC address bytes
    uint8_t mac[6];
    WiFi.macAddress(mac);

    // Build the raw 12-char MAC string (no colons)
    snprintf(_mac, sizeof(_mac),
        "%02X%02X%02X%02X%02X%02X",
        mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    // Build device_id = "SH-" + full 12-char MAC
    snprintf(_deviceId, sizeof(_deviceId), "%s%s", DEVICE_ID_PREFIX, _mac);

    Serial.printf("[DeviceIdentity] MAC: %s\n", _mac);
    Serial.printf("[DeviceIdentity] Device ID: %s\n", _deviceId);
}

const char* DeviceIdentity::getDeviceId() const {
    return _deviceId;
}

const char* DeviceIdentity::getMac() const {
    return _mac;
}
