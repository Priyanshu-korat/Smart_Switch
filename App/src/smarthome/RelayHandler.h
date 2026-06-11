// =============================================================================
// RelayHandler.h — 4-channel relay GPIO output control
// =============================================================================

#pragma once
#include <Arduino.h>

class RelayHandler {
public:
    // Initialise all relay GPIO pins as OUTPUT and restore last saved states
    void begin();

    // Set a single relay. switchIndex: 1..4, state: true = ON, false = OFF
    void setRelay(int switchIndex, bool state);

    // Toggle a single relay. Returns the new state.
    bool toggleRelay(int switchIndex);

    // Get the current in-memory state of a relay
    bool getRelayState(int switchIndex) const;

    // Set all 4 relays at once
    void setAllRelays(bool sw1, bool sw2, bool sw3, bool sw4);

    // Get all states packed as bitmask (bit0=sw1 ... bit3=sw4)
    uint8_t getAllStates() const;

private:
    bool _state[5] = {false};  // index 1..4 used; index 0 unused

    // Writes the physical GPIO without touching storage
    void _writePin(int switchIndex, bool state);
};

// Singleton instance
extern RelayHandler Relay;
