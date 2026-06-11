// =============================================================================
// InputHandler.h — 4-channel physical wall switch input with interrupt + debounce
//
// When a user physically presses a wall switch:
//   1. GPIO interrupt fires instantly (zero missed presses)
//   2. 50ms debounce window filters out contact noise
//   3. Relay is toggled
//   4. A publish callback is called so MqttManager can report the new state
// =============================================================================

#pragma once
#include <Arduino.h>

// Callback type: called when a physical switch changes state
// Parameters: switchIndex (1..4), newState (true=ON, false=OFF)
typedef void (*SwitchPhysicalCallback)(int switchIndex, bool newState);

class InputHandler {
public:
    // Initialise all input GPIO pins and attach interrupts.
    // The callback is called (from loop, not from ISR) whenever a
    // physical switch changes its debounced state.
    void begin(SwitchPhysicalCallback callback);

    // Must be called every loop() iteration to process pending ISR events
    void loop();

private:
    SwitchPhysicalCallback _callback = nullptr;

    // ISR flags — written by interrupt, read by loop()
    // Using volatile because they are modified in ISR context
    volatile bool _triggered[5] = {false};  // index 1..4
    volatile unsigned long _lastTriggerMs[5] = {0};

    // Debounced state tracking
    bool _lastState[5] = {false};  // index 1..4

    static void IRAM_ATTR _isr1();
    static void IRAM_ATTR _isr2();
    static void IRAM_ATTR _isr3();
    static void IRAM_ATTR _isr4();
};

// Singleton instance
extern InputHandler Input;
