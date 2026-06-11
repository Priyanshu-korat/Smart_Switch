// =============================================================================
// main.cpp — Application Layer
//
// This is the ONLY file you need to write as a product developer.
// The SmartHome library handles:
//   ✓ WiFi provisioning (SoftAP + REST API)
//   ✓ AWS MQTT connection and messaging
//   ✓ Physical switch input with interrupt + debounce
//   ✓ Relay GPIO control with state persistence
//   ✓ Full state machine lifecycle
//
// You just define what happens when a switch changes state.
// =============================================================================

#include "smarthome/SmartHomeCore.h"

// =============================================================================
// Application callback — called whenever any switch changes state
//
// This is triggered by BOTH:
//   (a) An app command via MQTT
//   (b) A physical wall switch press
//
// switchIndex: 1..4
// state:       true = ON, false = OFF
// =============================================================================
void onSwitchChanged(int switchIndex, bool state) {
    // Example: log the change to Serial
    Serial.printf("[App] Switch %d is now %s\n", switchIndex, state ? "ON" : "OFF");

    // Add your custom application logic here:
    // - Trigger scenes (e.g., Switch 1 ON + Switch 2 ON → dim Switch 3)
    // - Log to external service
    // - Control non-relay outputs (LEDs, buzzers, etc.)
}

// =============================================================================
void setup() {
    // Register your application callback BEFORE Core.begin()
    Core.onSwitchCommand(onSwitchChanged);

    // Boot the state machine — this call blocks until WiFi is connected
    // (or until provisioning mode is active)
    Core.begin();
}

// =============================================================================
void loop() {
    // Run the state machine every cycle
    Core.loop();
}
