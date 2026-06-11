// =============================================================================
// InputHandler.cpp
// =============================================================================

#include "InputHandler.h"
#include "config.h"
#include "RelayHandler.h"

// Singleton definition
InputHandler Input;

// Maps switch index (1..4) to its GPIO input pin
static const uint8_t INPUT_PINS[5] = {
    0,             // index 0 — unused
    INPUT_PIN_1,   // Switch 1
    INPUT_PIN_2,   // Switch 2
    INPUT_PIN_3,   // Switch 3
    INPUT_PIN_4    // Switch 4
};

// Static pointer to singleton — used by static ISR methods to access instance
static InputHandler* _instance = nullptr;

// =============================================================================
// ISR handlers — must be IRAM_ATTR and as short as possible
// They only record the timestamp; all logic runs in loop()
// =============================================================================
void IRAM_ATTR InputHandler::_isr1() {
    if (_instance) {
        _instance->_triggered[1] = true;
        _instance->_lastTriggerMs[1] = millis();
    }
}
void IRAM_ATTR InputHandler::_isr2() {
    if (_instance) {
        _instance->_triggered[2] = true;
        _instance->_lastTriggerMs[2] = millis();
    }
}
void IRAM_ATTR InputHandler::_isr3() {
    if (_instance) {
        _instance->_triggered[3] = true;
        _instance->_lastTriggerMs[3] = millis();
    }
}
void IRAM_ATTR InputHandler::_isr4() {
    if (_instance) {
        _instance->_triggered[4] = true;
        _instance->_lastTriggerMs[4] = millis();
    }
}

// =============================================================================
void InputHandler::begin(SwitchPhysicalCallback callback) {
    _instance = this;
    _callback = callback;

    for (int i = 1; i <= NUM_SWITCHES; i++) {
        pinMode(INPUT_PINS[i], INPUT_PULLUP);
        // Read initial state so we don't trigger a false event on first loop
        _lastState[i] = (digitalRead(INPUT_PINS[i]) == HIGH);
    }

    // Attach interrupts — CHANGE fires on both rising and falling edges
    attachInterrupt(digitalPinToInterrupt(INPUT_PINS[1]), _isr1, CHANGE);
    attachInterrupt(digitalPinToInterrupt(INPUT_PINS[2]), _isr2, CHANGE);
    attachInterrupt(digitalPinToInterrupt(INPUT_PINS[3]), _isr3, CHANGE);
    attachInterrupt(digitalPinToInterrupt(INPUT_PINS[4]), _isr4, CHANGE);

    Serial.println("[Input] Physical switch interrupts attached.");
}

// =============================================================================
// loop() — processes pending ISR events with debounce
// =============================================================================
void InputHandler::loop() {
    unsigned long now = millis();

    for (int i = 1; i <= NUM_SWITCHES; i++) {
        // Skip if no interrupt was triggered for this switch
        if (!_triggered[i]) continue;

        // Debounce: ignore if triggered again within debounce window
        if ((now - _lastTriggerMs[i]) < INPUT_DEBOUNCE_MS) continue;

        // Debounce passed — clear the flag and read the actual pin state
        _triggered[i] = false;
        bool currentState = (digitalRead(INPUT_PINS[i]) == HIGH);

        // Only act if the state actually changed (filter out noise)
        if (currentState == _lastState[i]) continue;
        _lastState[i] = currentState;

        Serial.printf("[Input] Physical switch %d -> %s\n", i, currentState ? "ON" : "OFF");

        // Toggle the relay to match the physical switch
        Relay.setRelay(i, currentState);

        // Notify the application layer (MqttManager will publish the state)
        if (_callback) {
            _callback(i, currentState);
        }
    }
}
