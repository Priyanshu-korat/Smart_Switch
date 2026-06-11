// =============================================================================
// SmartHomeCore.cpp — State machine orchestrator
// =============================================================================

#include "SmartHomeCore.h"
#include "config.h"
#include "DeviceIdentity.h"
#include "StorageManager.h"
#include "RelayHandler.h"
#include "InputHandler.h"
#include "ProvisioningServer.h"
#include "MqttManager.h"
#include <ESP8266WiFi.h>

// Singleton definition
SmartHomeCore Core;
SmartHomeCore* SmartHomeCore::_instance = nullptr;

// =============================================================================
void SmartHomeCore::onSwitchCommand(SwitchCommandCallback callback) {
    _switchCallback = callback;
}

// =============================================================================
// BOOT — initialise all subsystems, then decide next state
// =============================================================================
void SmartHomeCore::begin() {
    _instance = this;
    Serial.begin(SERIAL_BAUD);
    Serial.println("\n\n[Core] ============ SmartHome ESP8266 Booting ============");
    Serial.printf("[Core] Firmware: %s | Model: %s\n", FIRMWARE_VERSION, DEVICE_MODEL);

    // 1. Initialise device identity (reads MAC)
    DeviceID.begin();
    Serial.printf("[Core] Device ID: %s\n", DeviceID.getDeviceId());

    // 2. Initialise NVS storage
    Storage.begin();

    // 3. Initialise relay outputs — restores saved states
    Relay.begin();

    // 4. Decide: do we have saved WiFi credentials?
    if (Storage.hasWifiCredentials()) {
        Serial.println("[Core] Saved credentials found -> STATE: CONNECTING");
        _state = SystemState::CONNECTING;
        _enterConnecting();
    } else {
        Serial.println("[Core] No credentials -> STATE: PROVISIONING");
        _state = SystemState::PROVISIONING;
        _enterProvisioning();
    }
}

// =============================================================================
// MAIN LOOP
// =============================================================================
void SmartHomeCore::loop() {
    switch (_state) {

        case SystemState::PROVISIONING:
            Provisioning.loop();
            // Check AP timeout
            if (Provisioning.isTimedOut()) {
                Serial.println("[Core] Provisioning timeout. Rebooting...");
                delay(500);
                ESP.restart();
            }
            break;

        case SystemState::OPERATING:
            Input.loop();      // Process physical switch debounce
            Mqtt.loop();       // Maintain MQTT connection + heartbeat
            break;

        default:
            break;
    }
}

// =============================================================================
// STATE: PROVISIONING
// =============================================================================
void SmartHomeCore::_enterProvisioning() {
    // Scan networks BEFORE starting AP — this is the critical ordering
    Provisioning.scanNetworks();
    Provisioning.begin(DeviceID.getDeviceId());
    // From here, ProvisioningServer handles everything via loop()
    // On successful configure it calls ESP.restart() internally
}

// =============================================================================
// STATE: CONNECTING
// =============================================================================
void SmartHomeCore::_enterConnecting() {
    String ssid = Storage.getWifiSSID();
    String pass = Storage.getWifiPassword();

    Serial.printf("[Core] Connecting to WiFi: %s\n", ssid.c_str());
    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(true);
    WiFi.begin(ssid.c_str(), pass.c_str());

    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED) {
        if (millis() - start > WIFI_CONNECT_TIMEOUT_MS) {
            Serial.println("[Core] WiFi connect failed! Clearing credentials -> PROVISIONING");
            Storage.clearWifiCredentials();
            _state = SystemState::PROVISIONING;
            _enterProvisioning();
            return;
        }
        delay(500);
        Serial.print(".");
    }

    Serial.printf("\n[Core] WiFi connected! IP: %s\n", WiFi.localIP().toString().c_str());
    _enterOperating();
}

// =============================================================================
// STATE: OPERATING
// =============================================================================
void SmartHomeCore::_enterOperating() {
    _state = SystemState::OPERATING;
    Serial.println("[Core] STATE: OPERATING");

    // Attach physical input handler with callback
    Input.begin(_onPhysicalSwitch);

    // Attach MQTT manager with command callback
    Mqtt.setCommandCallback(_onMqttCommand);
    Mqtt.begin(DeviceID.getDeviceId());
}

// =============================================================================
// STATE: FACTORY RESET
// =============================================================================
void SmartHomeCore::_enterFactoryReset() {
    Serial.println("[Core] *** FACTORY RESET ***");
    Storage.factoryReset();
    WiFi.disconnect(true);
    delay(500);
    ESP.restart();
}

SystemState SmartHomeCore::getState() const {
    return _state;
}

// =============================================================================
// Callback: physical switch pressed
// Called from InputHandler after debounce
// =============================================================================
void SmartHomeCore::_onPhysicalSwitch(int switchIndex, bool newState) {
    // Relay was already toggled inside InputHandler.
    // Now publish the new state to MQTT with Req_type = REPORT
    int apiNo = switchIndex - 1;  // Convert 1-indexed to 0-indexed API_no
    Mqtt.publishState(REQ_TYPE_REPORT, apiNo, newState ? DATA_ON : DATA_OFF);

    // Also notify application layer
    if (_instance && _instance->_switchCallback) {
        _instance->_switchCallback(switchIndex, newState);
    }
}

// =============================================================================
// Callback: MQTT SET command received from app
// apiNo: 0..3 for individual relay, 10 for all
// data: 0=OFF, 1=ON
// =============================================================================
void SmartHomeCore::_onMqttCommand(int apiNo, int data) {
    bool state = (data == DATA_ON);

    if (apiNo == API_ALL_RELAYS) {
        // Set all relays to the same state
        // (A more nuanced "set all individually" would use a bitmask in Data)
        Relay.setAllRelays(state, state, state, state);
        if (_instance && _instance->_switchCallback) {
            for (int i = 1; i <= NUM_SWITCHES; i++) {
                _instance->_switchCallback(i, state);
            }
        }
    } else if (apiNo >= 0 && apiNo < NUM_SWITCHES) {
        int switchIndex = apiNo + 1;  // Convert 0-indexed API_no to 1-indexed
        Relay.setRelay(switchIndex, state);
        if (_instance && _instance->_switchCallback) {
            _instance->_switchCallback(switchIndex, state);
        }
    } else {
        Serial.printf("[Core] Unknown API_no: %d\n", apiNo);
    }
}
