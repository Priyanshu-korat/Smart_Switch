// =============================================================================
// StorageManager.cpp
// =============================================================================

#include "StorageManager.h"
#include "config.h"
#include <Preferences.h>

// Singleton definition
StorageManager Storage;

// NVS key names (kept short to save NVS space; max 15 chars)
static const char* KEY_WIFI_SSID   = "wifi_ssid";
static const char* KEY_WIFI_PASS   = "wifi_pass";
static const char* KEY_DEVICE_NAME = "dev_name";
static const char* KEY_SW1         = "sw1";
static const char* KEY_SW2         = "sw2";
static const char* KEY_SW3         = "sw3";
static const char* KEY_SW4         = "sw4";
static const char* KEY_WIFI_SAVED  = "wifi_saved";

static Preferences prefs;

// -----------------------------------------------------------------------------
void StorageManager::begin() {
    prefs.begin(NVS_NAMESPACE, false);  // false = read/write mode
    _initialized = true;
    Serial.println("[Storage] Initialized NVS namespace: " NVS_NAMESPACE);
}

// -----------------------------------------------------------------------------
// Wi-Fi Credentials
// -----------------------------------------------------------------------------
bool StorageManager::hasWifiCredentials() const {
    return prefs.getBool(KEY_WIFI_SAVED, false);
}

String StorageManager::getWifiSSID() const {
    return prefs.getString(KEY_WIFI_SSID, "");
}

String StorageManager::getWifiPassword() const {
    return prefs.getString(KEY_WIFI_PASS, "");
}

void StorageManager::saveWifiCredentials(const String& ssid, const String& password) {
    prefs.putString(KEY_WIFI_SSID, ssid);
    prefs.putString(KEY_WIFI_PASS, password);
    prefs.putBool(KEY_WIFI_SAVED, true);
    Serial.printf("[Storage] WiFi credentials saved. SSID: %s\n", ssid.c_str());
}

void StorageManager::clearWifiCredentials() {
    prefs.remove(KEY_WIFI_SSID);
    prefs.remove(KEY_WIFI_PASS);
    prefs.putBool(KEY_WIFI_SAVED, false);
    Serial.println("[Storage] WiFi credentials cleared.");
}

// -----------------------------------------------------------------------------
// Device Name
// -----------------------------------------------------------------------------
String StorageManager::getDeviceName() const {
    return prefs.getString(KEY_DEVICE_NAME, "SmartHome Device");
}

void StorageManager::saveDeviceName(const String& name) {
    prefs.putString(KEY_DEVICE_NAME, name);
    Serial.printf("[Storage] Device name saved: %s\n", name.c_str());
}

// -----------------------------------------------------------------------------
// Relay States
// -----------------------------------------------------------------------------
static const char* relayKey(int index) {
    switch (index) {
        case 1: return KEY_SW1;
        case 2: return KEY_SW2;
        case 3: return KEY_SW3;
        case 4: return KEY_SW4;
        default: return KEY_SW1;
    }
}

bool StorageManager::getRelayState(int switchIndex) const {
    return prefs.getBool(relayKey(switchIndex), false);
}

void StorageManager::saveRelayState(int switchIndex, bool state) {
    prefs.putBool(relayKey(switchIndex), state);
}

void StorageManager::saveAllRelayStates(bool sw1, bool sw2, bool sw3, bool sw4) {
    prefs.putBool(KEY_SW1, sw1);
    prefs.putBool(KEY_SW2, sw2);
    prefs.putBool(KEY_SW3, sw3);
    prefs.putBool(KEY_SW4, sw4);
}

// -----------------------------------------------------------------------------
// Factory Reset
// -----------------------------------------------------------------------------
void StorageManager::factoryReset() {
    prefs.clear();  // Wipes entire namespace in one call
    Serial.println("[Storage] Factory reset — all NVS keys cleared.");
}
