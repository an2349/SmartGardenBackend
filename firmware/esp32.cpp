#include <WiFi.h>
#include <WebServer.h>
#include <WebSocketsClient.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"

// ==================== PIN ====================
const int doamP = 34;     // Cảm biến độ ẩm đất (analog)
const int congtacP = 26;  // Relay bơm
const int ledP = 2;       // LED trạng thái

// ==================== Shared Data (giữa 2 core) ====================
Preferences preferences;
SemaphoreHandle_t dataMutex;

// Dữ liệu cảm biến (core 1 ghi, core 0 đọc để gửi)
volatile float currentDoam = 0;

// Cấu hình auto-water (Preferences + RAM)
volatile int autoMode = 0;        // 0=thủ công, 1=tự động
volatile float thresholdDoam = 30; // Ngưỡng độ ẩm (%)
volatile bool bomDangChay = false;
volatile unsigned long bomStartTime = 0;
const unsigned long BOM_TIMEOUT = 5 * 60 * 1000; // 5 phút fail-safe
const unsigned long SEND_INTERVAL = 15000; // 15 giây gửi telemetry
const unsigned long SENSOR_INTERVAL = 2000; // 2 giây đọc cảm biến

// ==================== Cấu hình mạng ====================
const char* ap_ssid = "caidat";
const char* ap_password = "";
WebServer server(80);

const char* server_host = "10.42.0.1";
const uint16_t server_port = 8080;
const char* ws_path = "/ws/device";
WebSocketsClient webSocket;

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
unsigned long lastMqttReconnect = 0;
const unsigned long MQTT_RECONNECT_INTERVAL = 5000;

String deviceMac = "";
String inputSSID = "", inputPASS = "", authCode = "", username = "", nameiot = "";
String mqttUser = "", mqttPass = "";
bool shouldRegister = false;
bool wifiConnected = false;
bool mqttConnected = false;

// ==================== Shared Data Helpers ====================
void setBom(bool on) {
  xSemaphoreTake(dataMutex, portMAX_DELAY);
  bomDangChay = on;
  if (on) bomStartTime = millis();
  xSemaphoreGive(dataMutex);
  digitalWrite(congtacP, on ? HIGH : LOW);
  digitalWrite(ledP, on ? HIGH : LOW);
}

bool getBomState() {
  bool state;
  xSemaphoreTake(dataMutex, portMAX_DELAY);
  state = bomDangChay;
  xSemaphoreGive(dataMutex);
  return state;
}

// ==================== Core 1: Sensor & Auto-Water ====================
void sensorTask(void *pvParameters) {
  Serial.println("[Core1] Sensor task started");

  while (1) {
    // Đọc cảm biến
    int analogValue = analogRead(doamP);
    float doam = map(analogValue, 4095, 0, 0, 100);
    if (doam < 0) doam = 0;
    if (doam > 100) doam = 100;

    xSemaphoreTake(dataMutex, portMAX_DELAY);
    currentDoam = doam;
    xSemaphoreGive(dataMutex);

    // Auto-water OFFLINE: tự quyết định bật/tắt bơm
    if (autoMode == 1) {
      bool bomOn = getBomState();
      if (doam < thresholdDoam && !bomOn) {
        Serial.print("[Core1] Auto ON - doam=");
        Serial.println(doam);
        setBom(true);
      } else if (doam >= thresholdDoam && bomOn) {
        Serial.print("[Core1] Auto OFF - doam=");
        Serial.println(doam);
        setBom(false);
      }
    }

    // Fail-safe: tự tắt bơm nếu chạy quá 5 phút
    if (getBomState()) {
      xSemaphoreTake(dataMutex, portMAX_DELAY);
      unsigned long elapsed = millis() - bomStartTime;
      xSemaphoreGive(dataMutex);
      if (elapsed > BOM_TIMEOUT) {
        Serial.println("[Core1] Fail-safe: tắt bơm (quá 5 phút)");
        setBom(false);
      }
    }

    vTaskDelay(SENSOR_INTERVAL / portTICK_PERIOD_MS);
  }
}

// ==================== Core 0: MQTT Callback ====================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String msg = String((char*)payload).substring(0, length);
  Serial.print("[Core0] MQTT: ");
  Serial.println(msg);

  StaticJsonDocument<200> doc;
  if (deserializeJson(doc, msg)) return;
  const char* command = doc["command"];

  if (command) {
    if (strcmp(command, "ON") == 0) {
      setBom(true);
      sendTelemetry();
      sendAck("ON");
    } else if (strcmp(command, "OFF") == 0) {
      setBom(false);
      sendAck("OFF");
    } else if (strcmp(command, "RESET") == 0) {
      Serial.println("[Core0] Lệnh RESET");
      delay(500);
      resetDevice();
    }
    return;
  }

  // Config update từ backend
  if (doc.containsKey("auto")) {
    autoMode = doc["auto"].as<int>();
    preferences.begin("wifi", false);
    preferences.putInt("auto", autoMode);
    preferences.end();
    Serial.printf("[Core0] Auto mode -> %d\n", autoMode);
  }
  if (doc.containsKey("threshold")) {
    thresholdDoam = doc["threshold"].as<float>();
    preferences.begin("wifi", false);
    preferences.putFloat("threshold", thresholdDoam);
    preferences.end();
    Serial.printf("[Core0] Threshold -> %.1f\n", thresholdDoam);
  }
}

void sendAck(const String& cmd) {
  StaticJsonDocument<100> ack;
  ack["command"] = cmd;
  ack["status"] = "OK";
  String s;
  serializeJson(ack, s);
  if (mqttClient.connected())
    mqttClient.publish((String("user/") + username + "/iot/" + deviceMac + "/ack").c_str(), s.c_str());
}

// ==================== Core 0: Gửi dữ liệu ====================
void sendTelemetry() {
  if (WiFi.status() != WL_CONNECTED) return;

  float doam;
  xSemaphoreTake(dataMutex, portMAX_DELAY);
  doam = currentDoam;
  xSemaphoreGive(dataMutex);

  StaticJsonDocument<200> doc;
  doc["mac"] = deviceMac;
  doc["doam"] = doam;
  doc["bom"] = getBomState() ? 1 : 0;
  doc["auto"] = autoMode;
  doc["threshold"] = thresholdDoam;

  String jsonStr;
  serializeJson(doc, jsonStr);

  if (mqttClient.connected())
    mqttClient.publish((String("user/") + username + "/iot/" + deviceMac + "/telemetry").c_str(), jsonStr.c_str(), true);
  if (webSocket.isConnected())
    webSocket.sendTXT(jsonStr);
}

// ==================== Core 0: MQTT Connect ====================
bool connectMQTT() {
  if (WiFi.status() != WL_CONNECTED) return false;
  String clientId = "esp32_" + deviceMac + "_" + String(random(0xffff), HEX);
  if (mqttClient.connect(clientId.c_str(), mqttUser.c_str(), mqttPass.c_str())) {
    mqttConnected = true;
    Serial.println("[Core0] MQTT connected");
    String prefix = "user/" + username + "/iot/" + deviceMac;
    String cmdTopic = prefix + "/command";
    String cfgTopic = prefix + "/config";
    mqttClient.subscribe(cmdTopic.c_str());
    mqttClient.subscribe(cfgTopic.c_str());
    Serial.printf("[Core0] MQTT subscribed: %s, %s\n", cmdTopic.c_str(), cfgTopic.c_str());

    StaticJsonDocument<100> s;
    s["online"] = true;
    String ss;
    serializeJson(s, ss);
    mqttClient.publish((prefix + "/status").c_str(), ss.c_str(), true);
    sendTelemetry();
    return true;
  }
  return false;
}

// ==================== WebSocket (Legacy) ====================
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  if (type == WStype_TEXT) {
    StaticJsonDocument<200> doc;
    if (deserializeJson(doc, payload, length)) return;
    const char* command = doc["command"];
    if (command) {
      if (strcmp(command, "ON") == 0) setBom(true);
      else if (strcmp(command, "OFF") == 0) setBom(false);
      else if (strcmp(command, "RESET") == 0) { delay(500); resetDevice(); }
    }
  } else if (type == WStype_CONNECTED) sendTelemetry();
}

// ==================== HTTP Retry ====================
bool sendHttpPost(const String& path, const String& body, int maxRetry = 3) {
  for (int i = 0; i < maxRetry; i++) {
    WiFiClient client;
    if (client.connect(server_host, server_port)) {
      String req = "POST " + path + " HTTP/1.1\r\nHost: " + String(server_host) + ":" + String(server_port) +
                   "\r\nContent-Type: application/json\r\nContent-Length: " + String(body.length()) +
                   "\r\nConnection: close\r\n\r\n" + body;
      client.print(req); client.flush();
      unsigned long timeout = millis() + 5000;
      while (!client.available() && millis() < timeout) delay(10);
      if (client.available()) {
        String resp = client.readString();
        if (resp.indexOf("200 OK") > 0 || resp.indexOf("201 Created") > 0) { client.stop(); return true; }
      }
      client.stop();
    }
    if (i < maxRetry - 1) delay(1000 * (i + 1));
  }
  return false;
}

// ==================== Core 0: Network Task ====================
void networkTask(void *pvParameters) {
  Serial.println("[Core0] Network task started");
  while (1) {
    if (WiFi.status() == WL_CONNECTED) {
      if (!mqttClient.connected()) {
        unsigned long now = millis();
        if (now - lastMqttReconnect > MQTT_RECONNECT_INTERVAL) {
          lastMqttReconnect = now;
          connectMQTT();
        }
      } else {
        mqttClient.loop();
      }
      webSocket.loop();

      // Gửi telemetry định kỳ
      static unsigned long lastSend = 0;
      if (millis() - lastSend > SEND_INTERVAL) {
        sendTelemetry();
        lastSend = millis();
      }
    }
    vTaskDelay(10 / portTICK_PERIOD_MS);
  }
}

// ==================== Web Server (AP mode) ====================
void handleRoot() { server.send(200, "text/html",
  "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width'><style>body{font-family:Arial;padding:20px;max-width:400px;margin:auto}input{width:100%;padding:8px;margin:5px 0;box-sizing:border-box}input[type=submit]{background:#4CAF50;color:white;border:none;padding:10px;cursor:pointer}</style></head>"
  "<body><h2> Đăng ký thiết bị</h2>"
  "<form action='/submit' method='POST'>"
  "Username: <input type='text' name='username' required><br>"
  "Tên thiết bị: <input type='text' name='name' required><br>"
  "SSID WiFi: <input type='text' name='ssid' required><br>"
  "Password: <input type='password' name='password'><br>"
  "MQTT User: <input type='text' name='mqtt_user' value='tuoicay'><br>"
  "MQTT Pass: <input type='password' name='mqtt_pass'><br>"
  "Mã xác thực: <input type='text' name='authcode' required><br>"
  "<input type='submit' value='Gửi'></form></body></html>"); }

void handleSubmit() {
  if (server.hasArg("ssid") && server.hasArg("authcode") && server.hasArg("name") && server.hasArg("username")) {
    inputSSID = server.arg("ssid"); inputPASS = server.arg("password");
    authCode = server.arg("authcode"); nameiot = server.arg("name"); username = server.arg("username");
    String mqttUserInput = server.arg("mqtt_user");
    String mqttPassInput = server.arg("mqtt_pass");
    preferences.begin("wifi", false);
    preferences.putString("ssid", inputSSID); preferences.putString("pass", inputPASS);
    preferences.putString("auth", authCode); preferences.putString("user", username);
    preferences.putString("nameiot", nameiot);
    if (!mqttUserInput.isEmpty()) preferences.putString("mqtt_user", mqttUserInput);
    if (!mqttPassInput.isEmpty()) preferences.putString("mqtt_pass", mqttPassInput);
    preferences.end();
    server.send(200, "text/plain", "Đã nhận. Thiết bị sẽ đăng ký và khởi động lại...");
    shouldRegister = true;
  } else server.send(400, "text/plain", "Thiếu thông tin.");
}

void startRegistrationAP() {
  WiFi.softAP(ap_ssid, ap_password);
  server.on("/", HTTP_GET, handleRoot);
  server.on("/submit", HTTP_POST, handleSubmit);
  server.begin();
  while (!shouldRegister) { server.handleClient(); delay(10); }
  server.stop(); WiFi.softAPdisconnect(true);
  WiFi.begin(inputSSID.c_str(), inputPASS.c_str());
  int timeout = 15000; unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < timeout) { delay(500); Serial.print("."); }
  if (WiFi.status() == WL_CONNECTED) {
    String mac = WiFi.macAddress();
    StaticJsonDocument<256> doc;
    doc["macId"] = mac; doc["username"] = username; doc["name"] = nameiot;
    doc["water"] = 1; doc["do_am"] = 30;
    String body; serializeJson(doc, body);
    sendHttpPost("/auth/dangkythietbi?authCode=" + authCode, body);
  }
  delay(1000); ESP.restart();
}

void resetDevice() {
  Serial.println("Xoá cấu hình và restart...");
  preferences.begin("wifi", false);
  preferences.clear();
  preferences.end();
  delay(1000); ESP.restart();
}

// ==================== Setup ====================
void setup() {
  Serial.begin(115200);
  Serial.println("\n\n=== Tuoicay ESP32 v3.0 Dual-Core ===");

  pinMode(ledP, OUTPUT); pinMode(congtacP, OUTPUT); pinMode(doamP, INPUT);
  digitalWrite(congtacP, LOW); digitalWrite(ledP, LOW);

  // Tạo mutex
  dataMutex = xSemaphoreCreateMutex();

  // Đọc cấu hình
  preferences.begin("wifi", true);
  String savedSSID = preferences.getString("ssid", "");
  inputSSID = preferences.getString("ssid", "");
  inputPASS = preferences.getString("pass", "");
  authCode = preferences.getString("auth", "");
  username = preferences.getString("user", "");
  nameiot = preferences.getString("nameiot", "");
  mqttUser = preferences.getString("mqtt_user", "");
  mqttPass = preferences.getString("mqtt_pass", "");
  if (mqttUser.isEmpty()) mqttUser = "tuoicay";
  if (mqttPass.isEmpty()) mqttPass = "tuoicay123";
  autoMode = preferences.getInt("auto", 0);
  thresholdDoam = preferences.getFloat("threshold", 30.0);
  preferences.end();

  if (savedSSID == "") { startRegistrationAP(); return; }

  WiFi.begin(savedSSID.c_str(), inputPASS.c_str());
  int timeout = 15000; unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < timeout) { delay(500); Serial.print("."); }
  if (WiFi.status() != WL_CONNECTED) { startRegistrationAP(); return; }
  deviceMac = WiFi.macAddress();

  // MQTT
  mqttClient.setServer(server_host, 1883);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setKeepAlive(30);

  // WebSocket legacy
  webSocket.begin(server_host, server_port, String(ws_path) + "?macId=" + deviceMac);
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(10000);

  // ===== Dual-Core: Core 0 = Network, Core 1 = Sensor =====
  xTaskCreatePinnedToCore(
    networkTask,   // Task function
    "Network",     // Name
    10000,         // Stack size
    NULL,          // Parameters
    1,             // Priority
    NULL,          // Task handle
    0              // Core 0 (PRO_CPU)
  );

  xTaskCreatePinnedToCore(
    sensorTask,
    "Sensor",
    4096,
    NULL,
    1,
    NULL,
    1              // Core 1 (APP_CPU)
  );

  Serial.printf("Dual-Core OK | Auto=%d Threshold=%.1f MAC=%s\n", autoMode, thresholdDoam, deviceMac.c_str());
}

// ==================== Loop (chỉ để không crash) ====================
void loop() {
  vTaskDelay(1000 / portTICK_PERIOD_MS);
}