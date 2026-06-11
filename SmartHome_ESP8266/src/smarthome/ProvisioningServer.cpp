// =============================================================================
// ProvisioningServer.cpp
// =============================================================================

#include "ProvisioningServer.h"
#include "config.h"
#include "StorageManager.h"
#include "DeviceIdentity.h"
#include <DNSServer.h>
#include <ESP8266WiFi.h>
#include <cJSON.h>

// Singleton definition
ProvisioningServer Provisioning;

static DNSServer dnsServer;
static const byte DNS_PORT = 53;

// Helper: send a JSON response
static void sendJson(ESP8266WebServer& server, int code, const char* json) {
    server.send(code, "application/json", json);
}

// =============================================================================
// STEP 1 — Scan networks BEFORE starting the AP
// =============================================================================
void ProvisioningServer::scanNetworks() {
    Serial.println("[Prov] Scanning for WiFi networks (before AP starts)...");
    WiFi.mode(WIFI_STA);
    int n = WiFi.scanNetworks();
    Serial.printf("[Prov] Found %d networks.\n", n);

    // Build the JSON array and cache it
    cJSON* root = cJSON_CreateObject();
    cJSON* arr  = cJSON_CreateArray();

    for (int i = 0; i < n; i++) {
        cJSON* net = cJSON_CreateObject();
        cJSON_AddStringToObject(net, "ssid",   WiFi.SSID(i).c_str());
        cJSON_AddNumberToObject(net, "rssi",   WiFi.RSSI(i));
        cJSON_AddBoolToObject  (net, "secure", (WiFi.encryptionType(i) != ENC_TYPE_NONE));
        cJSON_AddItemToArray(arr, net);
    }
    cJSON_AddItemToObject(root, "networks", arr);

    char* raw = cJSON_PrintUnformatted(root);
    _networksJson = String(raw);
    cJSON_free(raw);
    cJSON_Delete(root);

    WiFi.scanDelete();
}

// =============================================================================
// STEP 2 — Start SoftAP and REST API server
// =============================================================================
void ProvisioningServer::begin(const char* deviceId) {
    _deviceId = deviceId;
    _configured = false;
    _startMs = millis();

    // Build AP SSID: "SmartHome-Setup-AABBCCDDEEFF"
    String apSsid = String(AP_SSID_PREFIX) + String(deviceId).substring(3); // strip "SH-"
    Serial.printf("[Prov] Starting SoftAP: %s\n", apSsid.c_str());

    WiFi.mode(WIFI_AP);
    WiFi.softAP(apSsid.c_str());   // Open network — no password

    IPAddress apIP(192, 168, 4, 1);
    WiFi.softAPConfig(apIP, apIP, IPAddress(255, 255, 255, 0));

    // DNS: redirect all domains to 192.168.4.1 (captive portal)
    dnsServer.start(DNS_PORT, "*", apIP);

    // Register REST API endpoints
    _server.on("/api/info",            HTTP_GET,  [this]() { _handleInfo(); });
    _server.on("/api/wifi/networks",   HTTP_GET,  [this]() { _handleNetworks(); });
    _server.on("/api/wifi/configure",  HTTP_POST, [this]() { _handleConfigure(); });
    _server.onNotFound(               [this]() { _handleNotFound(); });

    _server.begin();
    _started = true;
    Serial.printf("[Prov] REST API listening at http://%s\n", AP_IP_ADDRESS);
}

// =============================================================================
void ProvisioningServer::loop() {
    if (!_started) return;
    dnsServer.processNextRequest();
    _server.handleClient();
}

// =============================================================================
void ProvisioningServer::stop() {
    if (!_started) return;
    _server.stop();
    dnsServer.stop();
    WiFi.softAPdisconnect(true);
    _started = false;
    Serial.println("[Prov] SoftAP stopped.");
}

// =============================================================================
bool ProvisioningServer::isTimedOut() const {
    return _started && (millis() - _startMs >= AP_TIMEOUT_MS);
}

bool ProvisioningServer::isConfigured() const {
    return _configured;
}

// =============================================================================
// REST HANDLERS
// =============================================================================
void ProvisioningServer::_handleInfo() {
    bool isProvisioned = Storage.hasWifiCredentials();

    cJSON* root = cJSON_CreateObject();
    cJSON_AddStringToObject(root, "device_id",        _deviceId);
    cJSON_AddStringToObject(root, "firmware_version", FIRMWARE_VERSION);
    cJSON_AddStringToObject(root, "model",            DEVICE_MODEL);
    cJSON_AddNumberToObject(root, "num_switches",     NUM_SWITCHES);
    cJSON_AddStringToObject(root, "status",           isProvisioned ? "provisioned" : "unprovisioned");
    cJSON_AddStringToObject(root, "mac",              DeviceID.getMac());

    char* payload = cJSON_PrintUnformatted(root);
    sendJson(_server, 200, payload);
    Serial.printf("[Prov] GET /api/info -> %s\n", payload);
    cJSON_free(payload);
    cJSON_Delete(root);
}

// =============================================================================
void ProvisioningServer::_handleNetworks() {
    // Serve the pre-scanned cached list — no live scan here
    sendJson(_server, 200, _networksJson.c_str());
    Serial.println("[Prov] GET /api/wifi/networks -> served cached list.");
}

// =============================================================================
void ProvisioningServer::_handleConfigure() {
    // Parse JSON body
    String body = _server.arg("plain");
    if (body.isEmpty()) {
        sendJson(_server, 400, "{\"success\":false,\"error\":\"MISSING_BODY\",\"message\":\"Request body is empty.\"}");
        return;
    }

    cJSON* root = cJSON_Parse(body.c_str());
    if (!root) {
        sendJson(_server, 400, "{\"success\":false,\"error\":\"INVALID_JSON\",\"message\":\"Could not parse JSON body.\"}");
        return;
    }

    cJSON* jSsid   = cJSON_GetObjectItem(root, "ssid");
    cJSON* jPass   = cJSON_GetObjectItem(root, "password");
    cJSON* jName   = cJSON_GetObjectItem(root, "device_name");

    if (!cJSON_IsString(jSsid) || !cJSON_IsString(jPass)) {
        cJSON_Delete(root);
        sendJson(_server, 400, "{\"success\":false,\"error\":\"MISSING_FIELDS\",\"message\":\"Fields 'ssid' and 'password' are required.\"}");
        return;
    }

    String ssid     = jSsid->valuestring;
    String password = jPass->valuestring;
    String devName  = cJSON_IsString(jName) ? jName->valuestring : "SmartHome Device";
    cJSON_Delete(root);

    Serial.printf("[Prov] POST /api/wifi/configure -> SSID: %s\n", ssid.c_str());

    // Attempt connection while still in AP mode (WIFI_AP_STA)
    ConfigureResult result = _attemptConnection(ssid, password);

    switch (result) {
        case ConfigureResult::SUCCESS:
            Storage.saveWifiCredentials(ssid, password);
            Storage.saveDeviceName(devName);
            sendJson(_server, 200,
                "{\"success\":true,\"message\":\"Credentials saved. Device will reboot in 2 seconds.\"}");
            Serial.println("[Prov] WiFi configure SUCCESS. Scheduling reboot...");
            _configured = true;
            // Reboot after a short delay so the HTTP response can be sent
            delay(2000);
            ESP.restart();
            break;

        case ConfigureResult::AUTH_FAILED:
            sendJson(_server, 401,
                "{\"success\":false,\"error\":\"WIFI_AUTH_FAILED\",\"message\":\"Wrong password. Please try again.\"}");
            break;

        case ConfigureResult::SSID_NOT_FOUND:
            sendJson(_server, 404,
                "{\"success\":false,\"error\":\"WIFI_NOT_FOUND\",\"message\":\"Network not found. Ensure 2.4GHz is enabled on your router.\"}");
            break;

        case ConfigureResult::TIMEOUT:
            sendJson(_server, 408,
                "{\"success\":false,\"error\":\"WIFI_TIMEOUT\",\"message\":\"Connection timed out. Please try again.\"}");
            break;
    }
}

// =============================================================================
// Attempt WiFi connection — blocking, returns result
// Runs in WIFI_AP_STA mode so the AP stays up throughout
// =============================================================================
ConfigureResult ProvisioningServer::_attemptConnection(const String& ssid, const String& password) {
    WiFi.mode(WIFI_AP_STA);
    WiFi.setAutoReconnect(false);
    WiFi.begin(ssid.c_str(), password.c_str());

    Serial.printf("[Prov] Attempting connection to: %s ...\n", ssid.c_str());
    unsigned long start = millis();

    while (millis() - start < WIFI_CONNECT_TIMEOUT_MS) {
        wl_status_t status = WiFi.status();

        if (status == WL_CONNECTED) {
            Serial.printf("[Prov] Connected! IP: %s\n", WiFi.localIP().toString().c_str());
            WiFi.disconnect(false);  // Disconnect STA — full connection happens after reboot
            return ConfigureResult::SUCCESS;
        }

        if (status == WL_WRONG_PASSWORD) {
            WiFi.disconnect(false);
            return ConfigureResult::AUTH_FAILED;
        }

        if (status == WL_NO_SSID_AVAIL) {
            WiFi.disconnect(false);
            return ConfigureResult::SSID_NOT_FOUND;
        }

        delay(200);
        Serial.print(".");
    }

    WiFi.disconnect(false);
    Serial.println();
    return ConfigureResult::TIMEOUT;
}

// =============================================================================
// Captive portal fallback — redirect unknown paths to /api/info
// =============================================================================
void ProvisioningServer::_handleNotFound() {
    _server.sendHeader("Location", "http://192.168.4.1/api/info", true);
    _server.send(302, "text/plain", "");
}
