# SmartHome Android App

This is the Android companion application for the Smart Switch IoT system. It provides a sleek, dark-themed dashboard to monitor, provision, and control ESP8266-based smart devices across a local network and over the internet via MQTT.

## Features

- **Real-time Synchronization:** Built with Kotlin Flows and Room to ensure that any physical change to a switch instantly reflects in the UI.
- **Offline Command Queueing:** Uses `WorkManager` to cache user commands when the network drops and silently retries them once connectivity is restored.
- **Device Provisioning UI:** A step-by-step wizard to walk users through connecting new hardware devices to their local Wi-Fi.
- **Modern UI Architecture:** 100% Jetpack Compose UI layered over a Clean Architecture (MVVM + Repository + UseCases).
- **Push Notifications:** Firebase Cloud Messaging (FCM) integration for background alerts.

## Requirements

- **Android Studio:** Ladybug (or newer recommended)
- **Java:** JDK 17
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 34 (Android 14)

## Installation and Setup

1. **Clone the Repository** and open the `App/android` folder in Android Studio.
2. **Setup Firebase:**
   - Go to the [Firebase Console](https://console.firebase.google.com/) and create a project.
   - Register the Android app with the package name `com.smarthome.app`.
   - Download the `google-services.json` file and place it in the `App/android/app/` directory.
3. **Build & Run:**
   - Click the green Run button (▶️) in Android Studio to deploy to your emulator or physical device.

## Core Technologies

- **UI:** Jetpack Compose, Material 3
- **Database:** Room (SQLite)
- **Dependency Injection:** Hilt / Dagger
- **Networking/IoT:** Eclipse Paho MQTT client
- **Background Tasks:** AndroidX WorkManager
- **Coroutines & Flows:** Kotlinx Coroutines

## Testing

You can use the built-in mock database to test the UI seamlessly without requiring actual hardware. To do this, simply launch the app; if the database is empty, it will auto-populate with virtual devices (e.g., "Living Room Hub", "Bedroom Lamp").
