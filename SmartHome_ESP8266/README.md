# SmartHome ESP8266 Firmware

This folder contains the firmware required to run the Smart Switch hardware devices based on the ESP8266 microcontroller (e.g., NodeMCU, Wemos D1 Mini).

## Overview

The firmware enables the ESP8266 to:
1. Act as a SoftAP (Access Point) during the initial provisioning phase, allowing the Android App to pass it the local Wi-Fi credentials.
2. Connect to the local Wi-Fi network.
3. Establish a secure MQTT connection to an IoT broker (e.g., AWS IoT Core).
4. Listen for incoming JSON payloads from the Android App to turn physical GPIO pins (relays) `ON` or `OFF`.
5. Publish its current state back to the MQTT broker so the Android App stays synchronized.

## Prerequisites

- **Hardware:** ESP8266 Development Board (NodeMCU, Wemos D1 Mini, etc.)
- **Relays:** 5V Relay Modules connected to the appropriate GPIO pins.
- **IDE:** PlatformIO (VS Code Extension) or Arduino IDE.

## Setup and Flashing

### Using PlatformIO (Recommended)
1. Open this folder (`SmartHome_ESP8266`) in VS Code with the PlatformIO extension installed.
2. PlatformIO will automatically download the necessary dependencies (like `PubSubClient`, `ArduinoJson`).
3. Connect your ESP8266 via USB.
4. Click the **Upload** button (the right arrow `→` on the bottom toolbar) to compile and flash the firmware.

### Configuration
Before flashing, you may need to update the MQTT broker endpoints and certificates in the source code to match your specific IoT Core instance.

## Provisioning

1. Once flashed, if the device cannot find a known Wi-Fi network, it will enter Provisioning Mode and broadcast a hotspot named `SmartHome_XXXXXX`.
2. Open the SmartHome Android App, click the **+ (Add Device)** button, and follow the step-by-step instructions to securely send your Home Wi-Fi credentials to the ESP8266.
3. The device will restart, connect to your router, and appear on your dashboard!
