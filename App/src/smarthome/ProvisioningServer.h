// =============================================================================
// ProvisioningServer.h — SoftAP + REST API + DNS Captive Portal
//
// REST API (served at http://192.168.4.1 while in SoftAP mode):
//
//   GET  /api/info              → device identity + status
//   GET  /api/wifi/networks     → cached pre-scanned network list
//   POST /api/wifi/configure    → submit SSID + password, blocks until result
// =============================================================================

#pragma once
#include <Arduino.h>
#include <ESP8266WebServer.h>

// Result of a configure attempt
enum class ConfigureResult {
    SUCCESS,
    AUTH_FAILED,
    SSID_NOT_FOUND,
    TIMEOUT
};

class ProvisioningServer {
public:
    // Scan WiFi networks — call BEFORE starting SoftAP (critical!)
    // Results are cached and served via /api/wifi/networks
    void scanNetworks();

    // Start SoftAP and HTTP server
    // deviceId: used to build the AP SSID and info response
    void begin(const char* deviceId);

    // Call every loop() iteration while in provisioning mode
    void loop();

    // Stop the server and SoftAP (called when provisioning completes)
    void stop();

    // Returns true if the SoftAP timeout has expired (5 minutes of no activity)
    bool isTimedOut() const;

    // Returns true if a successful configure has been received
    bool isConfigured() const;

private:
    const char* _deviceId = nullptr;
    ESP8266WebServer _server{80};
    bool _configured = false;
    bool _started = false;
    unsigned long _startMs = 0;

    // Cached network scan result (built before AP starts)
    String _networksJson;

    // Handlers
    void _handleInfo();
    void _handleNetworks();
    void _handleConfigure();
    void _handleNotFound();

    ConfigureResult _attemptConnection(const String& ssid, const String& password);
};

// Singleton instance
extern ProvisioningServer Provisioning;
