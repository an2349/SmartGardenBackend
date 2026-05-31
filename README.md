# Mini Smart Garden System v3.0

Hệ thống tưới cây từ xa, bao gồm **Backend Spring Boot** (Java) và **Firmware ESP32 Dual-Core** (Arduino/C++).
ESP32 tự động tưới OFFLINE dựa trên threshold local. 

---

## Kiến Trúc Hệ Thống

```
                          ┌──────────────────────────────┐
                          │        MQTT Broker           │
                          │     (Mosquitto/EMQX)         │
                          │       port 1883              │
                          └───┬──────────────┬───────────┘
                              │              │
           publish:           │              │  subscribe:
     iot/{mac}/telemetry ◄────┤              ├────► iot/{mac}/command
       iot/{mac}/status       │              │      iot/{mac}/config
       iot/{mac}/ack          │              │
                              │              │
                     ┌────────┴──┐    ┌──────┴─────────┐
                     │   ESP32   │    │    Backend      │
                     │ Dual-Core │    │  Spring Boot    │
                     │           │    │    :8080        │
                     │ Core 0:   │    │                 │
                     │  Network  │    │ - REST API      │
                     │ Core 1:   │    │ - JWT Auth      │
                     │  Sensor   │    │ - Telegram Bot  │
                     │  Auto-Water│   │ - Lịch tưới     │
                     └───────────┘    └─────────────────┘
```

---

##  Auto-Water OFFLINE 

### Cách hoạt động
- Ngưỡng độ ẩm (`threshold`) và chế độ (`auto`) lưu trong Preferences (non-volatile)
- Backend **chỉ gửi config** 
- Khi mất WiFi, ESP32 vẫn tưới tự động bình thường

### Luồng:
```
1. User bật auto = 1, threshold = 40% qua API:
   POST /iot/config/{macId}?auto=1&threshold=40

2. Backend gửi MQTT topic iot/{mac}/config:
   {"auto":1, "threshold":40}

3. ESP32 (Core 1) mỗi 2 giây:
   - Đọc cảm biến
   - Nếu độ ẩm < threshold AND bơm đang tắt → BẬT bơm
   - Nếu độ ẩm >= threshold AND bơm đang chạy → TẮT bơm
   - KHÔNG cần chờ server

4. ESP32 (Core 0) mỗi 15 giây:
   - Gửi dữ liệu lên server (để giám sát)
   - Nhận lệnh ON/OFF/RESET/config từ MQTT
```

---

##  Công Nghệ

### Backend
- **Java 17** + **Spring Boot 3.1.5**
- **Spring Data JPA** + Hibernate 6
- **Spring Security** + BCrypt + **JWT (jjwt 0.12)**
- **Spring Integration MQTT** (Eclipse Paho v5)
- **Spring WebSocket** (legacy comapt)
- **H2 Database** (nhúng)

### ESP32 Firmware
- **Arduino Framework** (C++)
- **FreeRTOS** (xTaskCreatePinnedToCore)
- **PubSubClient** (MQTT)
- **WebSocketsClient** (legacy)
- **ArduinoJson** + **Preferences**

---

## 🌐 API Endpoints

### Authentication (`/auth`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/auth/login` | Đăng nhập (trả về accessToken + refreshToken) |
| POST | `/auth/register` | Đăng ký tài khoản (BCrypt) |
| POST | `/auth/refresh` | Làm mới access token |
| POST | `/auth/dangkythietbi?authCode=` | ESP32 đăng ký thiết bị lần đầu |

### Thiết bị IoT (`/iot`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/iot/devices` | Danh sách thiết bị |
| GET | `/iot/{id}` | Chi tiết thiết bị |
| POST | `/iot/add` | Thêm thiết bị |
| POST | `/iot/fix` | Cập nhật thiết bị |
| DELETE | `/iot/{deviceId}` | Xoá thiết bị |
| POST | `/iot/control/bom/{deviceId}?command=ON&duration=30` | Điều khiển bơm + tưới thời lượng |
| **POST** | **`/iot/config/{deviceId}?auto=1&threshold=40`** | **Gửi config xuống ESP32** |

### Dữ liệu & Giám sát

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/iot/data/{deviceId}` | Dữ liệu cảm biến |
| GET | `/iot/data/{deviceId}/stats?from=&to=` | Thống kê |
| GET | `/iot/doam/{deviceId}` | Độ ẩm hiện tại |
| GET | `/iot/history/{deviceId}` | Lịch sử tưới |
| GET | `/iot/sensor/{deviceId}/doam` | Độ ẩm đất |
| GET | `/iot/sensor/{deviceId}/nhietdo` | Nhiệt độ |
| GET | `/iot/sensor/{deviceId}/doamkk` | Độ ẩm không khí |
| GET | `/iot/sensor/{deviceId}/anhsang` | Ánh sáng |
| GET | `/iot/sensor/{deviceId}/luuluong` | Lưu lượng nước |
| GET | `/iot/status/{deviceId}` | Trạng thái online/offline |

###  Lịch tưới & Chia sẻ

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET/POST/PUT/DELETE | `/iot/schedule/...` | CRUD lịch tưới |
| GET/POST/DELETE | `/iot/share/...` | Chia sẻ thiết bị |
| GET | `/iot/shared` | Thiết bị được chia sẻ với tôi |
| GET/POST | `/iot/alert/...` | Cảnh báo Telegram |

###  Người dùng (`/users`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET/PUT/DELETE | `/users/...` | CRUD user |
| GET | `/users/devices` | Thiết bị của tôi |
| GET | `/users/code` | Lấy mã auth code (6 số) |

---

##  Cấu Trúc Dự Án

```
Backend/
├── pom.xml
├── src/main/java/com/andeptrai/iot/
│   ├── TuoicayApplication.java        # @EnableScheduling
│   ├── config/
│   │   ├── MqttConfig.java            # Spring Integration MQTTv5
│   │   └── WebSocketConfig.java       # Legacy backward compat
│   ├── controller/
│   │   ├── AuthController.java        # Login/Register/Refresh/DeviceReg
│   │   ├── DeviceController.java      # IoT CRUD + Config + Sensor APIs
│   │   └── UserController.java        # User CRUD + Auth code
│   ├── jwt/
│   │   ├── JwtUtil.java               # Access + Refresh token
│   │   ├── JwtFilter.java             # JWT filter
│   │   └── SecurityConfig.java        # BCrypt, permit WS
│   ├── model/
│   │   ├── User.java, Iot.java
│   │   ├── SensorData.java, WateringHistory.java
│   │   ├── Schedule.java, DeviceShare.java, AlertRule.java
│   ├── repository/
│   │   ├── UserRepository.java, IotRepository.java
│   │   ├── SensorDataRepository.java, WateringHistoryRepository.java
│   │   ├── ScheduleRepository.java, DeviceShareRepository.java
│   │   └── AlertRuleRepository.java
│   ├── service/
│   │   ├── DeviceService.java         # Core: CRUD, MQTT, Config, Share, Alert
│   │   ├── UserService.java           # Auth, BCrypt, AuthCode
│   │   ├── MqttHandlerService.java    # Telemetry, Status, ACK
│   │   ├── ScheduleService.java       # Cron tưới định kỳ
│   │   ├── AlertService.java          # Telegram cảnh báo
│   │   └── DataCleanupService.java    # Dọn DB 3h sáng
│   └── websocket/
│       └── LegacyWebSocketHandler.java  # Cho ESP32 firmware cũ
│
├── src/arduino.cpp                    # ESP32 Firmware v3.0 Dual-Core
│
├── application.yml                    # JWT, MQTT, Telegram config
└── Dockerfile (tuỳ chọn)
```

---

##  Chạy Dự Án

### 1. MQTT Broker
```bash
docker run -d --name mosquitto -p 1883:1883 eclipse-mosquitto
```

### 2. Backend
```bash
# Build
mvn clean package -DskipTests

# Chạy (cần MQTT broker đang chạy)
java -jar target/tuoicay-2.0.0.jar
```

### 3. Cấu hình Telegram (tuỳ chọn)
```bash
# Tạo bot qua BotFather, lấy token
# Tìm chat ID (gửi tin nhắn rồi truy cập api.telegram.org/...)
java -jar target/tuoicay-2.0.0.jar \
  --telegram.bot-token=123456:ABC \
  --telegram.chat-id=123456
```

### 4. ESP32
1. Cài Arduino IDE + board ESP32 + thư viện: `PubSubClient`, `WebSocketsClient`, `ArduinoJson`
2. Nạp `src/arduino.cpp`
3. Kết nối WiFi AP `"caidat"` → nhập thông tin

---

##  ESP32 Dual-Core Chi Tiết

```
Core 0 (PRO_CPU) - Network Task:
├── MQTT connect + loop + reconnect
├── WebSocket (legacy)
├── Gửi telemetry mỗi 15s
├── Nhận lệnh ON/OFF/RESET
└── Nhận config (auto, threshold)

Core 1 (APP_CPU) - Sensor Task:
├── Đọc cảm biến mỗi 2s
├── Auto-water OFFLINE (so sánh threshold)
├── Fail-safe (tắt bơm sau 5 phút)
└── Chia sẻ dữ liệu qua mutex
```

---

##  Cấu Hình Môi Trường

| Biến | Mô tả | Mặc định |
|------|-------|----------|
| `BOT_TOKEN` | Telegram Bot Token | (trống) |
| `CHAT_ID` | Telegram Chat ID | (trống) |
| `MQTT_USER` | MQTT username | tuoicay |
| `MQTT_PASS` | MQTT password | tuoicay123 |

---

