// =============================================================================
// DeviceIdentity.h — Derives a unique device_id from the ESP8266 MAC address
// =============================================================================

#pragma once
#include <Arduino.h>

class DeviceIdentity {
public:
    // Call once in setup(). Reads MAC and builds the device_id string.
    void begin();

    // Returns the full device_id string, e.g. "SH-AABBCCDDEEFF"
    const char* getDeviceId() const;

    // Returns the raw MAC string without prefix, e.g. "AABBCCDDEEFF"
    const char* getMac() const;

private:
    char _deviceId[20];  // "SH-" + 12 hex chars + null terminator
    char _mac[14];       // 12 hex chars + null terminator
};

// Singleton instance — use DeviceID anywhere after DeviceID.begin()
extern DeviceIdentity DeviceID;
