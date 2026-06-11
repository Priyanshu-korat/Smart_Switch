// =============================================================================
// SmartHomeCore.h — State machine orchestrator
//
// Manages the firmware lifecycle:
//   STATE_BOOT        → Read NVS. Scan WiFi. Decide next state.
//   STATE_PROVISIONING → SoftAP + REST API. Wait for credentials.
//   STATE_CONNECTING  → Attempt WiFi connection with saved credentials.
//   STATE_OPERATING   → Normal mode: MQTT + relays + physical inputs.
//   STATE_FACTORY_RESET → Clear NVS and reboot.
// =============================================================================

#pragma once
#include <Arduino.h>

enum class SystemState {
    BOOT,
    PROVISIONING,
    CONNECTING,
    OPERATING,
    FACTORY_RESET
};

// Callback: called when an MQTT SET command arrives (from app or physical switch)
// Used by main.cpp to apply application-level logic
typedef void (*SwitchCommandCallback)(int switchIndex, bool state);

class SmartHomeCore {
public:
    // Registers the application-level switch command handler.
    // Call this BEFORE SmartHomeCore.begin()
    void onSwitchCommand(SwitchCommandCallback callback);

    // Initialises all subsystems and runs the boot state.
    // Call once in setup()
    void begin();

    // Runs the state machine. Call every iteration of loop()
    void loop();

    // Returns the current system state
    SystemState getState() const;

private:
    SystemState _state = SystemState::BOOT;
    SwitchCommandCallback _switchCallback = nullptr;

    void _enterProvisioning();
    void _enterConnecting();
    void _enterOperating();
    void _enterFactoryReset();

    // Callbacks passed into subsystems
    static void _onPhysicalSwitch(int switchIndex, bool newState);
    static void _onMqttCommand(int apiNo, int data);

    static SmartHomeCore* _instance;
};

// Singleton instance — use Core anywhere
extern SmartHomeCore Core;
