# SmartHome 4-Channel: App Developer Integration Guide

This document is the **single source of truth** for Mobile App Developers building the companion application for the SmartHome 4-Channel device. It is written to leave **zero** questions unanswered. Every edge case, failure scenario, timing issue, and UX decision is documented here.

---

## Table of Contents
1. [Device Identity System](#1-device-identity-system)
2. [Phase 1: Local Provisioning (SoftAP)](#2-phase-1-local-provisioning-softap)
   - [2.1 The "No Internet" Problem & Deferred Claiming Flow](#21-the-no-internet-problem--deferred-claiming-flow)
   - [2.2 REST API Specification](#22-rest-api-specification)
   - [2.3 Provisioning Corner Cases & Failure Handling](#23-provisioning-corner-cases--failure-handling)
3. [Phase 2: Cloud Operation (AWS MQTT)](#3-phase-2-cloud-operation-aws-mqtt)
   - [3.1 Topic Architecture](#31-topic-architecture)
   - [3.2 Payload Data Structure](#32-payload-data-structure)
   - [3.3 API Commands Reference](#33-api-commands-reference)
   - [3.4 Real-time Synchronization (Heartbeats)](#34-real-time-synchronization-heartbeats)
   - [3.5 MQTT Operation Corner Cases](#35-mqtt-operation-corner-cases)
4. [Phase 3: Physical Switch Handling](#4-phase-3-physical-switch-handling)
   - [4.1 Race Conditions: App Command vs Physical Press](#41-race-conditions-app-command-vs-physical-press)
5. [Phase 4: Power, Reset & Hardware Corner Cases](#5-phase-4-power-reset--hardware-corner-cases)
6. [Multi-User & Security Corner Cases](#6-multi-user--security-corner-cases)
7. [UX Best Practices](#7-ux-best-practices)

---

## 1. Device Identity System
Every device derives a globally unique ID from its hardware MAC address, which is burnt in at the factory and never changes.

- **MAC Format:** 12-character uppercase hex (e.g., `AABBCCDDEEFF`)
- **Device ID Format:** `SH-` prefix + full MAC (e.g., `SH-AABBCCDDEEFF`)

> **CRITICAL:** The `device_id` (`SH-AABBCCDDEEFF`) is the **primary key for everything**:
> - It forms the SoftAP network name: `SmartHome-Setup-AABBCCDDEEFF`
> - It is returned by the REST API during provisioning
> - It determines the MQTT topics the device subscribes/publishes to
> - It is the key stored in your backend database to link a device to a user account

**The device firmware generates its own `device_id` autonomously at boot from hardware.** The App never assigns or tells the device what its ID is. The App only reads and stores it.

---

## 2. Phase 1: Local Provisioning (SoftAP)
When the device is brand new OR factory reset, it enters **Provisioning Mode** and broadcasts a local Wi-Fi hotspot with a REST API for receiving Wi-Fi credentials.

### 2.1 The "No Internet" Problem & Deferred Claiming Flow

> ⚠️ **ROOT PROBLEM:** When the phone connects to the ESP8266's SoftAP to provision it, the phone **temporarily loses internet access**. This means all AWS Backend API calls (user registration, device claiming) are impossible during provisioning. This is not a bug — it is a hardware constraint of the phone's Wi-Fi radio.

**The Solution: Deferred Claiming Flow** — save the `device_id` locally during provisioning, then register it with AWS after internet is restored.

#### The Full Step-by-Step Flow:
1. **User action:** Plugs in the device for the first time. LED indicator blinks (provisioning mode).
2. **Device action:** Scans Wi-Fi, then starts broadcasting open network `SmartHome-Setup-AABBCCDDEEFF`. This AP has a **5-minute timeout**.
3. **App action (No Internet):** Detects the user wants to add a new device. Instructs user to go to phone Settings → Wi-Fi and connect to the `SmartHome-Setup-...` network.
4. **App action (No Internet):** Once connected, calls `GET /api/info` to confirm connection and save `device_id` and `device_name` input from user **into the phone's local memory / SQLite**. Do not discard this data under any circumstances.
5. **App action (No Internet):** Calls `GET /api/wifi/networks` to fetch scanned network list. Shows it to user.
6. **App action (No Internet):** User picks their home Wi-Fi, types password. App calls `POST /api/wifi/configure`. This request **blocks for up to 20 seconds** while the device verifies the credentials.
7. **Device action:** Attempts to connect to the router. Returns `200 OK` on success, or an error code on failure (see Section 2.3).
8. **Device action (after 200):** Saves credentials to non-volatile storage, then reboots. The SoftAP disappears.
9. **App action (Internet Restored):** The phone's OS detects the SoftAP is gone and automatically reverts to Home Wi-Fi or mobile data. The App now has internet. **Monitor for network change events** on both iOS and Android.
10. **App action (Internet Restored):** Call your AWS Backend API with the `device_id` from step 4 to register it under the logged-in user's account.
11. **App action (Internet Restored):** Navigate the user to the device dashboard. The device will be online in ~5-10 seconds after its reboot.

---

### 2.2 REST API Specification

**Base URL:** `http://192.168.4.1` (only reachable when phone is connected to the device's SoftAP)

> ⚠️ **No HTTPS.** The local provisioning API runs on plain HTTP. This is standard practice for device provisioning (same as Google Nest, Amazon Echo, etc.) because the phone and device are on the same local network and TLS certificate management on SoftAP is impractical.

#### `GET /api/info`
Confirm you are talking to the device and read the `device_id`.

**Response (200 OK):**
```json
{
  "device_id": "SH-AABBCCDDEEFF",
  "firmware_version": "1.0.0",
  "model": "SmartHome-4CH",
  "num_switches": 4,
  "status": "unprovisioned",
  "mac": "AABBCCDDEEFF"
}
```

> Note: `"status"` will be `"unprovisioned"` on first use, and `"provisioned"` if credentials are already saved (e.g., the device was already configured but the user is trying to re-provision it).

#### `GET /api/wifi/networks`
Returns the list of nearby 2.4GHz networks that the device scanned at boot (before the AP started).

**Response (200 OK):**
```json
{
  "networks": [
    { "ssid": "MyHomeRouter", "rssi": -45, "secure": true },
    { "ssid": "GuestNetwork", "rssi": -78, "secure": false }
  ]
}
```

> ⚠️ **This list is static.** It was scanned once at boot. If a network is missing, do NOT offer a "Refresh" button that re-scans, as scanning while in SoftAP mode causes radio instability. Instead show a message: *"Don't see your network? Ensure your router broadcasts on 2.4GHz."*

#### `POST /api/wifi/configure`
Submit Wi-Fi credentials. This is a **synchronous blocking call** — the device connects and responds before returning.

**Request Headers:** `Content-Type: application/json`

**Request Body:**
```json
{
  "ssid": "MyHomeRouter",
  "password": "mySuperSecretPassword123",
  "device_name": "Living Room"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Credentials saved. Device will reboot in 2 seconds."
}
```

**Set your HTTP client timeout to minimum 25 seconds** for this specific request, not the default 5-10 seconds.

---

### 2.3 Provisioning Corner Cases & Failure Handling

#### Case 1: Wrong Wi-Fi Password (401)
**What happens:** The router rejects the connection. Device responds immediately.
- **Response:** `HTTP 401 Unauthorized`
- **Body:** `{"success": false, "error": "WIFI_AUTH_FAILED", "message": "Wrong password."}`
- **App should:** Keep the user on the same password entry screen. Show toast: *"Incorrect Wi-Fi password. Please try again."*

#### Case 2: 5GHz Network / SSID Not Found (404)
**What happens:** The ESP8266 only supports 2.4GHz. The network was not found during connection attempt.
- **Response:** `HTTP 404 Not Found`
- **Body:** `{"success": false, "error": "WIFI_NOT_FOUND", "message": "Network not found."}`
- **App should:** Show: *"Network not found. Your device only supports 2.4GHz Wi-Fi. Check that your router has 2.4GHz enabled."*

#### Case 3: Router Too Far Away / Connection Timeout (408)
**What happens:** The device can see the SSID but is too far from the router to connect.
- **Response:** `HTTP 408 Request Timeout`
- **Body:** `{"success": false, "error": "WIFI_TIMEOUT", "message": "Connection timed out."}`
- **App should:** Show: *"Connection timed out. Move the device closer to your router and try again."*

#### Case 4: App HTTP Request Times Out Before Device Responds
**What happens:** The device is still trying to connect, but the app's own HTTP client has a shorter timeout (e.g., 5 seconds) and gives up.
- **What the device does:** It will keep trying for up to 20 seconds, but nobody is listening for the result. After attempting, the device stays alive and does NOT reboot (no credentials saved). The SoftAP is still running.
- **App should:** Set HTTP timeout to **25 seconds** for `/api/wifi/configure` only. If your framework forces you to use a shorter timeout, check the SoftAP is still visible and retry.

#### Case 5: App is Killed/Crashes During Provisioning
**What happens:** The user backgrounds the app or the OS kills it during steps 4-9.
- **If killed before step 7 (configure POST):** The device still has its SoftAP up (within 5 min). The user can re-open the app and restart the process from step 3.
- **If killed after 200 OK (step 7) but before step 10:** The `device_id` saved in phone memory is gone. The device is now on the home Wi-Fi and connected to AWS, but the user's account has no record of it.
- **App should:** Persist the `device_id` to **disk** (local DB/SQLite), not just RAM, as soon as it is read in step 4. Then check for "pending claiming" entries on app launch and retry the AWS registration if internet is available.

#### Case 6: SoftAP Times Out (5 Minutes)
**What happens:** The device shuts down the SoftAP if no configure request is received within 5 minutes.
- **Device action:** Reboots automatically. If no credentials are saved, it starts provisioning mode again.
- **App should:** Poll `GET /api/info` every 10 seconds while on the provisioning screen. If all requests fail for 20+ seconds, show: *"Device setup timed out. Please power cycle the device and try again."*

#### Case 7: Device Already Has Saved Credentials (Re-Provisioning)
**What happens:** The user is trying to set up a device that was already configured (e.g., they moved house and the old Wi-Fi is gone).
- **GET /api/info** will return `"status": "provisioned"`.
- **The configure endpoint still works** — posting new credentials overwrites the old ones.
- **App should:** Show a warning: *"This device is already set up. Continuing will overwrite the existing Wi-Fi settings."*

#### Case 8: AWS Backend Registration Fails After Internet Restored
**What happens:** Step 10 (registering device_id to user account) fails due to server error, network issue, or auth token expiry.
- **App should:** Save the unregistered `device_id` in the local pending queue on disk. Retry automatically every 30 seconds until it succeeds. Show a non-intrusive persistent banner: *"'Living Room' device is being registered..."*

#### Case 9: Network Scan Returns Empty List
**What happens:** The device scanned at boot and found no networks (rare — can happen in RF-noisy environments).
- **`/api/wifi/networks`** returns `{"networks": []}`.
- **App should:** Show an input field for manual SSID entry instead of the list. Let the user type their SSID and password manually.

#### Case 10: Multiple Users Try to Provision the Same Device
**What happens:** Two family members both scan the SoftAP simultaneously.
- Only one person's `POST /api/wifi/configure` will win — whichever arrives first gets the 200 OK response. The second will get a 200 OK response only if sent within the active SoftAP window.
- **The real danger:** Both users register the same `device_id` to their respective accounts on the backend. You must enforce ownership on the backend (first registration wins; reject subsequent claims of the same `device_id`).

---

## 3. Phase 2: Cloud Operation (AWS MQTT)
Once provisioned and rebooted, the device connects to home Wi-Fi and then to AWS IoT Core. All communication is now over MQTT over TLS.

### 3.1 Topic Architecture
There are exactly **two MQTT topics** per device. This ensures complete data isolation between different users and devices.

| Direction | Topic | Purpose |
|---|---|---|
| App → Device | `smarthome/{device_id}/cmd` | App sends GET/SET commands |
| Device → App | `smarthome/{device_id}/state` | Device sends confirmations and REPORT events |

> **Why topics are per-device:** Because the `device_id` is in the topic path, messages for `SH-AABBCCDDEEFF` are physically unreachable by `SH-112233445566`. There is no way for one user to accidentally control another's device, even if both subscribe to `smarthome/#` — your backend AWS IoT Policy must be configured to only allow each user to subscribe to their own devices' topics.

### 3.2 Payload Data Structure
Every message in both directions uses this exact JSON schema:

```json
{
  "Device_ID": "SH-AABBCCDDEEFF",
  "Req_type": 1,
  "API_no": 0,
  "Data": 1
}
```

#### Field Glossary:
| Field | Type | Values |
|---|---|---|
| `Device_ID` | String | The 15-char device ID |
| `Req_type` | Int | `0`=GET, `1`=SET, `2`=REPORT |
| `API_no` | Int | `0`=Relay1, `1`=Relay2, `2`=Relay3, `3`=Relay4, `10`=All |
| `Data` | Int | `0`=OFF, `1`=ON (or bitmask for API_no=10) |

**Bitmask for API_no=10 (All Relays):**
- `Data` is a 4-bit integer where each bit represents one relay: `bit0`=Relay1, `bit1`=Relay2, `bit2`=Relay3, `bit3`=Relay4.
- Examples: `Data=0` → all OFF. `Data=15` → all ON. `Data=5` (binary `0101`) → Relay1 ON, Relay2 OFF, Relay3 ON, Relay4 OFF.

### 3.3 API Commands Reference

**Turn ON Relay 2:**
- App → `/cmd`: `{ "Device_ID": "SH-...", "Req_type": 1, "API_no": 1, "Data": 1 }`
- Device → `/state`: `{ "Device_ID": "SH-...", "Req_type": 1, "API_no": 1, "Data": 1 }`

**Turn OFF Relay 3:**
- App → `/cmd`: `{ "Device_ID": "SH-...", "Req_type": 1, "API_no": 2, "Data": 0 }`
- Device → `/state`: `{ "Device_ID": "SH-...", "Req_type": 1, "API_no": 2, "Data": 0 }`

**Query Relay 1 state:**
- App → `/cmd`: `{ "Device_ID": "SH-...", "Req_type": 0, "API_no": 0, "Data": 0 }`
- Device → `/state`: `{ "Device_ID": "SH-...", "Req_type": 0, "API_no": 0, "Data": 1 }` (currently ON)

**Query all relay states (on App open):**
- App → `/cmd`: `{ "Device_ID": "SH-...", "Req_type": 0, "API_no": 10, "Data": 0 }`
- Device → `/state`: `{ "Device_ID": "SH-...", "Req_type": 0, "API_no": 10, "Data": 9 }` (binary 1001 = Relay1 ON, Relay2 OFF, Relay3 OFF, Relay4 ON)

### 3.4 Real-time Synchronization (Heartbeats)
The device publishes a heartbeat every **60 seconds** to `/state` using `Req_type: 2`, `API_no: 10`, with the current bitmask state of all relays.

**App responsibilities:**
- Track the timestamp of the last received heartbeat per device.
- If no heartbeat or state message is received for **90 seconds**, mark the device as **"Offline"** in the UI.
- When the device comes back online, the next heartbeat will arrive and should trigger the UI to switch back to **"Online"** and update all switch states.

---

### 3.5 MQTT Operation Corner Cases

#### Case 11: App Sends Command but Device is Offline (No Confirmation)
**What happens:** The App publishes to `/cmd` but the device is off or disconnected from Wi-Fi. No response arrives on `/state`.
- **App should:** Show a loading/pending state for maximum **3 seconds**. If no confirmation arrives, revert the switch UI to its previous state and show toast: *"Device unreachable. Check if it is powered on."*

#### Case 12: App Just Opened — MQTT Subscribed, No State Known Yet
**What happens:** On app startup, the MQTT connection is established. The device might not send its next heartbeat for up to 60 seconds. The app does not know the current switch states.
- **App should:** Immediately publish a GET all (`Req_type: 0`, `API_no: 10`) to `/cmd` right after MQTT is connected. Show a loading shimmer on all 4 switches until the response arrives.

#### Case 13: App Receives a Message While in Background (Push Notification)
**What happens:** A physical switch is pressed while the app is backgrounded. The MQTT client is disconnected (as per best practice).
- The state change is NOT received by the app in real time.
- **On foreground:** The app must publish `Req_type: 0`, `API_no: 10` to GET all states. This re-syncs the UI.
- **If you want push notifications for physical presses:** Your backend MQTT listener (AWS Lambda triggered by IoT Rule on `smarthome/+/state`) should detect `Req_type: 2` messages and push an FCM/APNs notification to the user's device.

#### Case 14: Two Apps (Same User, Two Phones) Send Commands Simultaneously
**What happens:** The user's spouse is also using the app. Both send SET commands for Relay 1 within milliseconds.
- **Device behavior:** Processes messages in order received. The last one wins. Device publishes the final confirmed state to `/state`.
- **App behavior:** Both phones receive the final `/state` confirmation and update their UI. Because both listen to the same `/state` topic, they will self-correct within one round-trip.
- **No special handling needed** — the `/state` listener is your single source of truth.

#### Case 15: Device Wi-Fi Drops During Normal Operation
**What happens:** The home Wi-Fi router reboots, or the device temporarily loses signal.
- **Device behavior:** `WiFi.setAutoReconnect(true)` is set. It will reconnect automatically and re-establish the MQTT connection. The firmware is programmed to retry MQTT connection 3 times, then reboot if still failing.
- **App behavior:** The device will go "Offline" after 90 seconds (missed heartbeat). When Wi-Fi and MQTT reconnect, the device automatically publishes its current state as a heartbeat. App detects the next heartbeat and marks the device "Online" again.

#### Case 16: MQTT Message Received Out of Order
**What happens:** Network latency causes a stale confirmation to arrive after a newer one.
- **Example:** App turns relay ON, receives confirmation ON. Then turns it OFF. The stale ON confirmation from the first command arrives again.
- **App should:** Ignore any incoming `/state` message whose `Req_type` is `1` (SET confirmation) if the device is not currently in a "pending" state for that specific relay. Use a per-relay "awaiting confirmation" flag and clear it once the first confirmation arrives.

#### Case 17: Device Reboots Due to MQTT Retry Exhaustion
**What happens:** Device loses internet and exhausts all 3 MQTT retry attempts. It reboots.
- After reboot, it reads saved Wi-Fi credentials, reconnects, and re-joins MQTT.
- Relay states are restored from NVS (non-volatile storage) — **the physical relays will return to exactly the same state they were in before the reboot**.
- **App behavior:** The device will appear "Offline" for ~20-30 seconds (reboot + reconnect time), then come back "Online" with a heartbeat. No action required from the App.

#### Case 18: Router is Replaced / Wi-Fi Credentials Change
**What happens:** The user gets a new router. The saved SSID/password on the device is now wrong. The device cannot connect to Wi-Fi and cannot reach AWS.
- **Device behavior:** Wi-Fi connect fails. Device falls into provisioning mode after failure.
- **App should:** Detect the device has been "Offline" for an extended period (e.g., 10+ minutes). Show a prompt: *"'Living Room' has been offline for a while. If you changed your Wi-Fi router, you may need to re-setup the device."* Then guide the user through provisioning again (Section 2).
- **On re-provisioning:** The same `device_id` is retained (it's based on MAC, not credentials). No backend changes needed. The existing device-to-user link in AWS remains valid.

---

## 4. Phase 3: Physical Switch Handling
The board has 4 physical wall switch input pins. When a user presses a physical switch, the firmware **instantly** toggles the relay and sends a REPORT to AWS.

**Device publishes to `/state`:**
```json
{
  "Device_ID": "SH-AABBCCDDEEFF",
  "Req_type": 2,
  "API_no": 0,
  "Data": 0
}
```
(`Req_type: 2` = REPORT, `API_no: 0` = Relay 1, `Data: 0` = now OFF)

> **App MUST:** Listen to the `/state` topic at all times while the dashboard screen is visible. On receiving `Req_type: 2`, instantly animate the UI switch to match `Data` without any confirmation step.

### 4.1 Race Conditions: App Command vs Physical Press

#### Case 19: User Taps App Switch AND Presses Physical Switch at the Same Time
**What happens:** The app sends SET ON. 5ms later, the physical switch toggles OFF. The device processes both in sequence: first turns ON (from app), then turns OFF (from physical press). The final state is OFF.
- **Device publishes:** `Req_type: 1` (SET ON confirmation), then `Req_type: 2` (REPORT OFF).
- **App behavior:** First updates UI to ON (from SET confirmation). Then immediately updates UI back to OFF (from REPORT). This is correct behavior — the last REPORT from the device is truth.
- **UX note:** The switch may flicker briefly. This is expected and acceptable behavior for simultaneous input.

#### Case 20: App is in "Pending" State When a REPORT Arrives
**What happens:** App sent a SET command and is showing a loading spinner. Before the SET confirmation arrives, a physical switch REPORT arrives for the same relay.
- **App should:** Treat any `Req_type: 2` REPORT as the definitive current state, cancelling the pending operation. Update the UI to match the REPORT value immediately. This prevents the UI from getting stuck in "pending" forever.

---

## 5. Phase 4: Power, Reset & Hardware Corner Cases

#### Case 21: Power Outage During Normal Operation
**What happens:** Electricity cuts out. All relays go to the hardware-default state (OFF, since relay coils are de-energized without power).
- **On power restore:** The device boots, reads saved relay states from NVS (non-volatile storage), and **restores each relay to its last known state** (e.g., Relay 1 was ON → it turns back ON on reboot).
- **App behavior:** After 10-15 seconds, the device heartbeat arrives. The app UI updates automatically. No special handling needed.

#### Case 22: Power Outage DURING a Relay State Write (NVS Corruption Risk)
**What happens:** Power cuts out in the microsecond the firmware is writing to NVS. The stored state may be partially written.
- **The `vshymanskyy/Preferences` library** uses wear-levelled flash and atomic writes, greatly minimizing this risk. If corruption occurs, the NVS key is cleared and the relay defaults to OFF.
- **App behavior:** On next heartbeat, the app might show Relay 1 as OFF even though it was ON before the outage. This is the safe fail-state. The user can manually turn it back on from the app.

#### Case 23: Factory Reset
**What happens:** A hardware factory reset clears all saved credentials (Wi-Fi SSID/password, device name, relay states). The device enters provisioning mode again.
- **What the backend still has:** Your AWS account still has a record linking `SH-AABBCCDDEEFF` to the user. The MQTT topics remain valid.
- **App behavior after user re-provisions:** The device will reconnect to AWS with the same `device_id`. The app does NOT need to re-register the device. Simply guide the user through provisioning (Section 2) and they land back on the existing device dashboard.
- **If a different user takes ownership:** You must provide a backend "release device" / "transfer ownership" API. The previous owner's app must stop subscribing to this device's `/state` topic.

#### Case 24: Device First Boot Takes Longer Than Expected
**What happens:** On the very first boot after flashing the firmware, the NVS partition must be initialized. This adds ~2-3 seconds to boot time.
- **App behavior:** If a user tries to open the dashboard right after provisioning, the first heartbeat may take 15-20 seconds instead of the usual 5-10 seconds. Show a "Connecting..." spinner and wait at least 30 seconds before showing an "Offline" status.

---

## 6. Multi-User & Security Corner Cases

#### Case 25: Device Ownership Transfer (Selling / Giving Away the Device)
**What happens:** User A sells the device to User B. User A still has it in their app.
- **Your backend must:** Implement a "Remove Device" API that:
  1. Deletes the `device_id` → `userA` association in DynamoDB.
  2. Revokes the IoT Policy permission for User A's identity on that topic.
- **The new owner (User B)** factory resets the device and re-provisions it. The same `device_id` is now claimed by User B.
- **App (User A):** After removal, the device must disappear from User A's dashboard.

#### Case 26: Malicious User Tries to Guess Another Device's Topic
**What happens:** A bad actor knows the pattern `smarthome/SH-XXXXXXXXXXXX/cmd` and tries to control a device they don't own.
- **Your AWS IoT Policy must enforce:** Each authenticated user can only publish/subscribe to `smarthome/{their_device_ids}/*`. This is configured in AWS IoT Core policies, not in the app or firmware.
- **The App is not responsible for this.** This is a backend/infrastructure concern. But be aware it must be done.

#### Case 27: Two Users Claim the Same Device (Backend Conflict)
**What happens:** User A and User B both completed provisioning of the same device (e.g., using two phones simultaneously). Both call your backend to register the same `device_id`.
- **Your backend must:** Enforce that a `device_id` can only be owned by one user at a time. The first registration wins. Return an error to the second.
- **App (second user):** Show: *"This device has already been registered by another account. Please perform a factory reset on the device before adding it to your account."*

---

## 7. UX Best Practices

### Command Confirmation (Pessimistic UI)
Do **not** optimistically update the switch UI. Always wait for device confirmation.
1. User taps switch → Show loading spinner on that switch.
2. Publish `Req_type: 1` (SET).
3. Start a 3-second countdown timer.
4. If confirmation arrives: Update UI, clear spinner.
5. If 3 seconds pass with no confirmation: Revert switch, show toast: *"Device unreachable."*

### App Foreground Resume — Always Sync State
Every time the app comes to the foreground from background:
1. Reconnect MQTT if disconnected.
2. Immediately publish `Req_type: 0`, `API_no: 10` (GET all).
3. Show loading shimmers on all 4 switches until the response arrives.

### Background App — Disconnect MQTT
When the app is backgrounded or minimised:
- Disconnect the MQTT client gracefully.
- On foreground, reconnect and run the foreground sync above.
- This saves phone battery (no persistent TCP connection).

### Online / Offline Indicator
- Show a colored dot (green=online, gray=offline) per device on the dashboard.
- Mark as **Offline** if no message received on `/state` for **90 seconds**.
- Mark as **Online** as soon as any message arrives on `/state`.

### Device Name Display
- During provisioning (step 4), the user types a `device_name` (e.g., "Living Room").
- This name is stored on the device's NVS AND on your backend.
- Always display the user-given name in the app, not the raw `device_id`.

### Handling the Network Scan "Refresh" Request
Users may ask to refresh the Wi-Fi list shown during provisioning.
- **Do NOT** add a refresh button. The device cannot re-scan while its AP is active (radio conflict).
- Instead, add an instructional message: *"The list was scanned when the device was powered on. If your network is missing, ensure your router broadcasts on 2.4GHz."*

### Handle Simultaneous Physical + App Input Gracefully
- The app must always treat the **most recently received `/state` message** as ground truth.
- Never "lock" a switch's UI state — always allow it to be overridden by an incoming REPORT.
