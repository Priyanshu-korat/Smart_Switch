// =============================================================================
// MqttManager.h — AWS IoT Core MQTT stub with clean integration interface
//
// INTEGRATION NOTE FOR WHEN CERT CODE IS AVAILABLE:
//   1. Replace the placeholder strings in MqttManager.cpp:
//      - AWS_ENDPOINT
//      - ROOT_CA_CERT
//      - DEVICE_CERT
//      - PRIVATE_KEY
//   2. The rest of the class works without any changes.
//
// PAYLOAD FORMAT (Generic):
//   Every message uses this structure:
//   { "Device_ID": "SH-...", "Req_type": 0|1|2, "API_no": 0-10, "Data": 0|1 }
//
//   Req_type: 0 = GET, 1 = SET, 2 = REPORT (physical switch triggered)
//   API_no:   0=Relay1, 1=Relay2, 2=Relay3, 3=Relay4, 10=All
//   Data:     0=OFF, 1=ON  (or [1,0,1,0] array for API_no=10)
// =============================================================================

#pragma once
#include <Arduino.h>

// Callback type: called when a SET command arrives from the app
// Parameters: apiNo (relay index 0..3 or 10 for all), data (0=OFF, 1=ON)
typedef void (*MqttCommandCallback)(int apiNo, int data);

class MqttManager {
public:
    // Initialise — call after WiFi is connected.
    // deviceId: the full device_id string from DeviceIdentity
    void begin(const char* deviceId);

    // Must be called every loop() iteration to maintain MQTT connection
    // and process incoming messages
    void loop();

    // Returns true if currently connected to AWS IoT Core
    bool isConnected() const;

    // Publish a state report to the state topic.
    // reqType: REQ_TYPE_SET or REQ_TYPE_REPORT
    // apiNo:   API_RELAY_1..4 or API_ALL_RELAYS
    // data:    DATA_ON or DATA_OFF
    void publishState(int reqType, int apiNo, int data);

    // Convenience: publish all relay states at once
    void publishAllStates();

    // Publish device online heartbeat
    void publishHeartbeat();

    // Register callback for incoming SET commands
    void setCommandCallback(MqttCommandCallback callback);

private:
    const char* _deviceId = nullptr;
    char _cmdTopic[64];    // smarthome/{device_id}/cmd
    char _stateTopic[64];  // smarthome/{device_id}/state
    bool _stubMode = true; // true until real certs are integrated

    MqttCommandCallback _commandCallback = nullptr;
    unsigned long _lastHeartbeatMs = 0;
    unsigned long _lastReconnectAttemptMs = 0;
    int _reconnectCount = 0;

    bool _connect();
    void _onMessage(const char* topic, uint8_t* payload, unsigned int length);

    // Static shim for PubSubClient callback
    static void _messageShim(const char* topic, uint8_t* payload, unsigned int length);
    static MqttManager* _instance;
};

// Singleton instance
extern MqttManager Mqtt;
