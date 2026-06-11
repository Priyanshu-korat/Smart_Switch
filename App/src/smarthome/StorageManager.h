// =============================================================================
// StorageManager.h — Persistent key-value storage using Preferences.h (NVS)
//
// Wraps the ESP8266 Preferences library for clean read/write of:
//   - Wi-Fi credentials
//   - Device name (set during provisioning)
//   - Last known relay states (restored after reboot)
// =============================================================================

#pragma once
#include <Arduino.h>

class StorageManager {
public:
    // Call once in setup()
    void begin();

    // --- Wi-Fi Credentials ---
    bool hasWifiCredentials() const;
    String getWifiSSID() const;
    String getWifiPassword() const;
    void saveWifiCredentials(const String& ssid, const String& password);
    void clearWifiCredentials();

    // --- Device Name (set by user in app during provisioning) ---
    String getDeviceName() const;
    void saveDeviceName(const String& name);

    // --- Relay States (persisted across reboots) ---
    // switchIndex: 1..4
    bool getRelayState(int switchIndex) const;
    void saveRelayState(int switchIndex, bool state);
    void saveAllRelayStates(bool sw1, bool sw2, bool sw3, bool sw4);

    // --- Factory Reset — wipes ALL stored values ---
    void factoryReset();

private:
    bool _initialized = false;
};

// Singleton instance
extern StorageManager Storage;
