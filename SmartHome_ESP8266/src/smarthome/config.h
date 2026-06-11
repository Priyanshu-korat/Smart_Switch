// =============================================================================
// config.h — Central configuration for SmartHome ESP8266 Firmware
//
// INSTRUCTIONS:
//   This is the ONLY file you need to edit to configure the firmware for
//   your specific hardware. Update the GPIO pin numbers below to match
//   your actual board wiring before flashing.
// =============================================================================

#pragma once

// -----------------------------------------------------------------------------
// RELAY OUTPUT PINS
// These control the physical relay coils (HIGH = relay energised / ON)
// Update these #defines to match your actual PCB wiring.
// -----------------------------------------------------------------------------
#define RELAY_PIN_1   D1   // Switch 1 relay output — update as needed
#define RELAY_PIN_2   D2   // Switch 2 relay output — update as needed
#define RELAY_PIN_3   D5   // Switch 3 relay output — update as needed
#define RELAY_PIN_4   D6   // Switch 4 relay output — update as needed

// Set to LOW if your relay module is active-low (common with optocoupler boards)
#define RELAY_ON_STATE    HIGH
#define RELAY_OFF_STATE   LOW

// -----------------------------------------------------------------------------
// PHYSICAL INPUT PINS
// These read the real-world wall switch state (INPUT_PULLUP recommended)
// Update these #defines to match your actual PCB wiring.
// -----------------------------------------------------------------------------
#define INPUT_PIN_1   D7   // Physical switch 1 input — update as needed
#define INPUT_PIN_2   D8   // Physical switch 2 input — update as needed
#define INPUT_PIN_3   D3   // Physical switch 3 input — update as needed
#define INPUT_PIN_4   D4   // Physical switch 4 input — update as needed

// Debounce time in milliseconds (ignore rapid re-triggers within this window)
#define INPUT_DEBOUNCE_MS   50

// -----------------------------------------------------------------------------
// DEVICE IDENTITY
// -----------------------------------------------------------------------------
#define DEVICE_MODEL        "SmartHome-4CH"
#define FIRMWARE_VERSION    "1.0.0"
#define DEVICE_ID_PREFIX    "SH-"   // Prepended to the full MAC address

// -----------------------------------------------------------------------------
// SOFT ACCESS POINT (PROVISIONING MODE)
// -----------------------------------------------------------------------------
#define AP_SSID_PREFIX      "SmartHome-Setup-"  // Full SSID = prefix + device_id
// Open network — no password (industry standard for provisioning APs)
#define AP_IP_ADDRESS       "192.168.4.1"
#define AP_SUBNET           "255.255.255.0"

// How long (ms) to stay in provisioning mode before auto-reboot if no activity
#define AP_TIMEOUT_MS       300000UL  // 5 minutes

// -----------------------------------------------------------------------------
// WI-FI STATION (NORMAL OPERATION)
// -----------------------------------------------------------------------------
// How long (ms) to wait for WiFi connection before declaring failure
#define WIFI_CONNECT_TIMEOUT_MS   20000UL  // 20 seconds

// -----------------------------------------------------------------------------
// MQTT (AWS IoT Core)
// NOTE: Cert strings are defined in MqttManager.cpp (stub placeholders).
//       Replace them with your real certs when integrating.
// -----------------------------------------------------------------------------
#define MQTT_PORT           8883   // AWS IoT Core TLS port
#define MQTT_RETRY_COUNT    3      // Retry attempts before rebooting
#define MQTT_HEARTBEAT_MS   60000UL  // 60 seconds between heartbeat publishes
#define MQTT_KEEPALIVE_S    60     // MQTT keepalive interval in seconds

// MQTT Topic Roots (device_id is appended at runtime)
#define MQTT_TOPIC_CMD_PREFIX    "smarthome/"    // + device_id + "/cmd"
#define MQTT_TOPIC_STATE_PREFIX  "smarthome/"    // + device_id + "/state"

// -----------------------------------------------------------------------------
// MQTT PAYLOAD — Req_type values
// -----------------------------------------------------------------------------
#define REQ_TYPE_GET     0   // App asks device for current status
#define REQ_TYPE_SET     1   // App commands device to change state
#define REQ_TYPE_REPORT  2   // Device voluntarily reports state change

// -----------------------------------------------------------------------------
// MQTT PAYLOAD — API_no values (Request IDs for this 4-channel board)
// -----------------------------------------------------------------------------
#define API_RELAY_1      0
#define API_RELAY_2      1
#define API_RELAY_3      2
#define API_RELAY_4      3
#define API_ALL_RELAYS   10   // Get/Set all relays at once

// -----------------------------------------------------------------------------
// DATA values
// -----------------------------------------------------------------------------
#define DATA_OFF  0
#define DATA_ON   1

// -----------------------------------------------------------------------------
// NUMBER OF CHANNELS
// -----------------------------------------------------------------------------
#define NUM_SWITCHES  4

// -----------------------------------------------------------------------------
// PREFERENCES (NVS) NAMESPACE
// -----------------------------------------------------------------------------
#define NVS_NAMESPACE "smarthome"

// -----------------------------------------------------------------------------
// SERIAL DEBUG
// -----------------------------------------------------------------------------
#define SERIAL_BAUD   115200
