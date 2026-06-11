# Smart Switch Monorepo

Welcome to the **Smart Switch** project repository! This monorepo contains the full software stack for an IoT Smart Home solution. It includes both the mobile application for user control and the firmware for the ESP8266 hardware devices.

## Repository Structure

- **[`/App`](./App/)**  
  The Android mobile application built using modern Android development practices (Kotlin, Jetpack Compose, Coroutines, Room, Hilt, and WorkManager). It acts as the central dashboard for the user to provision and control devices.
  
- **[`/SmartHome_ESP8266`](./SmartHome_ESP8266/)**  
  The C++ firmware for ESP8266 microcontrollers. It connects to the user's Wi-Fi network, subscribes to AWS IoT Core (or HiveMQ) MQTT topics, and physically toggles relays based on commands received from the Android App.

## Getting Started

To get started, please refer to the specific documentation located in each sub-project:

1. [Android App Documentation](./App/README.md)
2. [ESP8266 Firmware Documentation](./SmartHome_ESP8266/README.md)

## Architecture Overview

The system operates via an MQTT message broker:
1. The **Mobile App** sends a JSON command (e.g., toggle switch `0` to `ON`) to a specific device topic via MQTT.
2. The **ESP8266** device, which is subscribed to its unique topic, receives the command, toggles the physical GPIO relay, and publishes its updated state back to the broker.
3. The **Mobile App** receives the state update and instantly refreshes the user interface.

If the network drops, the mobile app leverages WorkManager to queue the commands offline and automatically retries when the connection is restored.
