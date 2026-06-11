// =============================================================================
// RelayHandler.cpp
// =============================================================================

#include "RelayHandler.h"
#include "config.h"
#include "StorageManager.h"

// Singleton definition
RelayHandler Relay;

// Maps switch index (1..4) to its GPIO pin number
static const uint8_t RELAY_PINS[5] = {
    0,              // index 0 — unused
    RELAY_PIN_1,    // Switch 1
    RELAY_PIN_2,    // Switch 2
    RELAY_PIN_3,    // Switch 3
    RELAY_PIN_4     // Switch 4
};

// -----------------------------------------------------------------------------
void RelayHandler::begin() {
    for (int i = 1; i <= NUM_SWITCHES; i++) {
        pinMode(RELAY_PINS[i], OUTPUT);

        // Restore last saved state from NVS so relays resume after a reboot
        bool saved = Storage.getRelayState(i);
        _state[i] = saved;
        _writePin(i, saved);

        Serial.printf("[Relay] Switch %d -> GPIO %d, restored state: %s\n",
            i, RELAY_PINS[i], saved ? "ON" : "OFF");
    }
}

// -----------------------------------------------------------------------------
void RelayHandler::setRelay(int switchIndex, bool state) {
    if (switchIndex < 1 || switchIndex > NUM_SWITCHES) return;

    _state[switchIndex] = state;
    _writePin(switchIndex, state);
    Storage.saveRelayState(switchIndex, state);  // Persist to NVS

    Serial.printf("[Relay] Switch %d set to %s\n", switchIndex, state ? "ON" : "OFF");
}

// -----------------------------------------------------------------------------
bool RelayHandler::toggleRelay(int switchIndex) {
    if (switchIndex < 1 || switchIndex > NUM_SWITCHES) return false;

    bool newState = !_state[switchIndex];
    setRelay(switchIndex, newState);
    return newState;
}

// -----------------------------------------------------------------------------
bool RelayHandler::getRelayState(int switchIndex) const {
    if (switchIndex < 1 || switchIndex > NUM_SWITCHES) return false;
    return _state[switchIndex];
}

// -----------------------------------------------------------------------------
void RelayHandler::setAllRelays(bool sw1, bool sw2, bool sw3, bool sw4) {
    bool states[5] = {false, sw1, sw2, sw3, sw4};
    for (int i = 1; i <= NUM_SWITCHES; i++) {
        _state[i] = states[i];
        _writePin(i, states[i]);
    }
    Storage.saveAllRelayStates(sw1, sw2, sw3, sw4);
    Serial.printf("[Relay] All relays set: SW1=%d SW2=%d SW3=%d SW4=%d\n",
        sw1, sw2, sw3, sw4);
}

// -----------------------------------------------------------------------------
uint8_t RelayHandler::getAllStates() const {
    uint8_t mask = 0;
    for (int i = 1; i <= NUM_SWITCHES; i++) {
        if (_state[i]) mask |= (1 << (i - 1));
    }
    return mask;
}

// -----------------------------------------------------------------------------
// Private: write GPIO without touching NVS
// -----------------------------------------------------------------------------
void RelayHandler::_writePin(int switchIndex, bool state) {
    digitalWrite(RELAY_PINS[switchIndex], state ? RELAY_ON_STATE : RELAY_OFF_STATE);
}
