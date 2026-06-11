// =============================================================================
// MqttManager.cpp — AWS IoT Core MQTT (stub mode until certs are integrated)
// =============================================================================

#include "MqttManager.h"
#include "config.h"
#include "RelayHandler.h"
#include "DeviceIdentity.h"
#include <PubSubClient.h>
#include <WiFiClientSecure.h>
#include <cJSON.h>

// =============================================================================
// *** INTEGRATION POINT — Replace these placeholders with your real certs ***
// =============================================================================
static const char* AWS_ENDPOINT = "REPLACE_WITH_YOUR_AWS_ENDPOINT.iot.REGION.amazonaws.com";

static const char ROOT_CA_CERT[] PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
REPLACE WITH YOUR AWS ROOT CA CERTIFICATE
-----END CERTIFICATE-----
)EOF";

static const char DEVICE_CERT[] PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
REPLACE WITH YOUR DEVICE CERTIFICATE
-----END CERTIFICATE-----
)EOF";

static const char PRIVATE_KEY[] PROGMEM = R"EOF(
-----BEGIN RSA PRIVATE KEY-----
REPLACE WITH YOUR DEVICE PRIVATE KEY
-----END RSA PRIVATE KEY-----
)EOF";
// =============================================================================
// END INTEGRATION POINT
// =============================================================================

// Singleton definition
MqttManager Mqtt;
MqttManager* MqttManager::_instance = nullptr;

static WiFiClientSecure wifiClient;
static PubSubClient mqttClient(wifiClient);

// -----------------------------------------------------------------------------
void MqttManager::begin(const char* deviceId) {
    _instance = this;
    _deviceId = deviceId;

    // Build topics
    snprintf(_cmdTopic,   sizeof(_cmdTopic),   "smarthome/%s/cmd",   _deviceId);
    snprintf(_stateTopic, sizeof(_stateTopic), "smarthome/%s/state", _deviceId);

    Serial.printf("[MQTT] CMD topic:   %s\n", _cmdTopic);
    Serial.printf("[MQTT] STATE topic: %s\n", _stateTopic);

    // Detect stub mode (certs not yet replaced)
    _stubMode = (strncmp(AWS_ENDPOINT, "REPLACE", 7) == 0);

    if (_stubMode) {
        Serial.println("[MQTT] *** STUB MODE *** Cert placeholders detected.");
        Serial.println("[MQTT] All other features (provisioning, relays, inputs) are fully operational.");
        Serial.println("[MQTT] Replace AWS_ENDPOINT and cert strings in MqttManager.cpp to enable cloud.");
        return;
    }

    // Configure TLS certificates using BearSSL
    static BearSSL::X509List cert(ROOT_CA_CERT);
    static BearSSL::X509List client_crt(DEVICE_CERT);
    static BearSSL::PrivateKey key(PRIVATE_KEY);

    wifiClient.setTrustAnchors(&cert);
    wifiClient.setClientRSACert(&client_crt, &key);

    mqttClient.setServer(AWS_ENDPOINT, MQTT_PORT);
    mqttClient.setKeepAlive(MQTT_KEEPALIVE_S);
    mqttClient.setCallback(_messageShim);

    Serial.println("[MQTT] TLS configured. Connecting to AWS IoT Core...");
    _connect();
}

// -----------------------------------------------------------------------------
bool MqttManager::_connect() {
    if (_stubMode) return false;
    if (mqttClient.connected()) return true;

    Serial.printf("[MQTT] Connecting as: %s\n", _deviceId);
    if (mqttClient.connect(_deviceId)) {
        mqttClient.subscribe(_cmdTopic);
        Serial.printf("[MQTT] Connected. Subscribed to: %s\n", _cmdTopic);
        publishHeartbeat();
        publishAllStates();
        _reconnectCount = 0;
        return true;
    }

    Serial.printf("[MQTT] Connect failed. State: %d\n", mqttClient.state());
    return false;
}

// -----------------------------------------------------------------------------
void MqttManager::loop() {
    if (_stubMode) return;

    if (!mqttClient.connected()) {
        unsigned long now = millis();
        // Non-blocking retry every 5 seconds
        if (now - _lastReconnectAttemptMs > 5000) {
            _lastReconnectAttemptMs = now;
            _reconnectCount++;
            Serial.printf("[MQTT] Reconnect attempt %d/%d\n", _reconnectCount, MQTT_RETRY_COUNT);

            if (_reconnectCount > MQTT_RETRY_COUNT) {
                Serial.println("[MQTT] Max retries exceeded. Rebooting...");
                delay(500);
                ESP.restart();
            }
            _connect();
        }
        return;
    }

    mqttClient.loop();

    // Heartbeat
    unsigned long now = millis();
    if (now - _lastHeartbeatMs >= MQTT_HEARTBEAT_MS) {
        _lastHeartbeatMs = now;
        publishHeartbeat();
    }
}

// -----------------------------------------------------------------------------
bool MqttManager::isConnected() const {
    if (_stubMode) return false;
    return mqttClient.connected();
}

// -----------------------------------------------------------------------------
// Build and publish a generic JSON payload to the state topic
// -----------------------------------------------------------------------------
void MqttManager::publishState(int reqType, int apiNo, int data) {
    if (_stubMode) {
        Serial.printf("[MQTT][STUB] Would publish -> Req_type:%d API_no:%d Data:%d\n",
            reqType, apiNo, data);
        return;
    }

    cJSON* root = cJSON_CreateObject();
    cJSON_AddStringToObject(root, "Device_ID", _deviceId);
    cJSON_AddNumberToObject(root, "Req_type",  reqType);
    cJSON_AddNumberToObject(root, "API_no",    apiNo);
    cJSON_AddNumberToObject(root, "Data",      data);

    char* payload = cJSON_PrintUnformatted(root);
    mqttClient.publish(_stateTopic, payload);
    Serial.printf("[MQTT] Published: %s\n", payload);

    cJSON_free(payload);
    cJSON_Delete(root);
}

// -----------------------------------------------------------------------------
// Publish all 4 relay states as a batch (API_no = 10, Data = bitmask)
// -----------------------------------------------------------------------------
void MqttManager::publishAllStates() {
    // Pack all relay states into a bitmask: bit0=sw1 ... bit3=sw4
    int bitmask = (int)Relay.getAllStates();
    publishState(REQ_TYPE_REPORT, API_ALL_RELAYS, bitmask);
}

// -----------------------------------------------------------------------------
// Heartbeat: publishes a SET with API_ALL_RELAYS showing current state
// The app uses this to keep UI in sync after reconnecting
// -----------------------------------------------------------------------------
void MqttManager::publishHeartbeat() {
    if (_stubMode) {
        Serial.println("[MQTT][STUB] Heartbeat tick.");
        return;
    }
    publishAllStates();
}

// -----------------------------------------------------------------------------
void MqttManager::setCommandCallback(MqttCommandCallback callback) {
    _commandCallback = callback;
}

// -----------------------------------------------------------------------------
// Static shim routes PubSubClient callback to singleton instance
// -----------------------------------------------------------------------------
void MqttManager::_messageShim(const char* topic, uint8_t* payload, unsigned int length) {
    if (_instance) _instance->_onMessage(topic, payload, length);
}

// -----------------------------------------------------------------------------
// Parse incoming generic JSON payload and dispatch relay command
// -----------------------------------------------------------------------------
void MqttManager::_onMessage(const char* topic, uint8_t* payload, unsigned int length) {
    // Null-terminate the payload
    char buf[256];
    if (length >= sizeof(buf)) {
        Serial.println("[MQTT] Incoming payload too large. Ignored.");
        return;
    }
    memcpy(buf, payload, length);
    buf[length] = '\0';

    Serial.printf("[MQTT] Received on [%s]: %s\n", topic, buf);

    // Parse JSON
    cJSON* root = cJSON_Parse(buf);
    if (!root) {
        Serial.println("[MQTT] JSON parse error. Ignored.");
        return;
    }

    cJSON* jReqType = cJSON_GetObjectItem(root, "Req_type");
    cJSON* jApiNo   = cJSON_GetObjectItem(root, "API_no");
    cJSON* jData    = cJSON_GetObjectItem(root, "Data");

    if (!cJSON_IsNumber(jReqType) || !cJSON_IsNumber(jApiNo) || !cJSON_IsNumber(jData)) {
        Serial.println("[MQTT] Missing required fields. Ignored.");
        cJSON_Delete(root);
        return;
    }

    int reqType = (int)jReqType->valuedouble;
    int apiNo   = (int)jApiNo->valuedouble;
    int data    = (int)jData->valuedouble;

    cJSON_Delete(root);

    // Only handle SET commands from the app (Req_type = 1)
    if (reqType != REQ_TYPE_SET) {
        // GET requests: respond with current state
        if (reqType == REQ_TYPE_GET) {
            if (apiNo == API_ALL_RELAYS) {
                publishAllStates();
            } else if (apiNo >= 0 && apiNo < NUM_SWITCHES) {
                bool state = Relay.getRelayState(apiNo + 1);  // API_no is 0-indexed
                publishState(REQ_TYPE_GET, apiNo, state ? DATA_ON : DATA_OFF);
            }
        }
        return;
    }

    // Dispatch SET command to callback
    if (_commandCallback) {
        _commandCallback(apiNo, data);
    }

    // Confirm state back to app
    if (apiNo == API_ALL_RELAYS) {
        publishAllStates();
    } else {
        bool state = Relay.getRelayState(apiNo + 1);
        publishState(REQ_TYPE_SET, apiNo, state ? DATA_ON : DATA_OFF);
    }
}
