package com.smartgardenmini.service;

import com.smartgardenmini.model.*;
import com.smartgardenmini.repository.*;
import com.smartgardenmini.websocket.LegacyWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final IotRepository iotRepo;
    private final UserRepository userRepo;
    private final SensorDataRepository sensorDataRepo;
    private final WateringHistoryRepository historyRepo;
    private final DeviceShareRepository shareRepo;
    private final AlertRuleRepository alertRuleRepo;
    private final @Lazy ScheduleService scheduleService;
    private final @Lazy LegacyWebSocketHandler legacyWebSocket;
    private final MessageChannel mqttOutputChannel;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // Online tracking: macId → lastSeen
    private final Map<String, LocalDateTime> lastSeen = new ConcurrentHashMap<>();
    // Command ACK tracking
    private final Map<String, CompletableFuture<Boolean>> pendingAcks = new ConcurrentHashMap<>();
    // Scheduled executor for delayed OFF commands
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public DeviceService(IotRepository iotRepo, UserRepository userRepo,
                         SensorDataRepository sensorDataRepo, WateringHistoryRepository historyRepo,
                         DeviceShareRepository shareRepo, AlertRuleRepository alertRuleRepo,
                         @Lazy ScheduleService scheduleService,
                         @Lazy LegacyWebSocketHandler legacyWebSocket,
                         MessageChannel mqttOutputChannel,
                         com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.iotRepo = iotRepo;
        this.userRepo = userRepo;
        this.sensorDataRepo = sensorDataRepo;
        this.historyRepo = historyRepo;
        this.shareRepo = shareRepo;
        this.alertRuleRepo = alertRuleRepo;
        this.scheduleService = scheduleService;
        this.legacyWebSocket = legacyWebSocket;
        this.mqttOutputChannel = mqttOutputChannel;
        this.objectMapper = objectMapper;
    }

    // ==================== Online Tracking ====================

    public void markOnline(String macId) {
        lastSeen.put(macId, LocalDateTime.now());
        iotRepo.findBymacId(macId).ifPresent(device -> {
            if (!device.isOnline()) {
                device.setOnline(true);
                iotRepo.save(device);
            }
        });
    }

    @Scheduled(fixedRate = 30000) // Mỗi 30 giây
    public void checkOfflineDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(120);
        lastSeen.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(threshold)) {
                iotRepo.findBymacId(entry.getKey()).ifPresent(device -> {
                    device.setOnline(false);
                    iotRepo.save(device);
                    log.info("Thiết bị {} đã offline", entry.getKey());
                });
                return true;
            }
            return false;
        });
    }

    public boolean isOnline(String macId) {
        return lastSeen.containsKey(macId)
                && lastSeen.get(macId).isAfter(LocalDateTime.now().minusSeconds(120));
    }

    // ==================== MQTT Commands ====================

    public void sendMqttCommand(String macId, String command) {
        sendMqttCommand(macId, command, null);
    }

    public void sendMqttCommand(String macId, String command, Integer durationSec) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("command", command);
            if (durationSec != null) {
                payload.put("duration", durationSec);
            }
            String username = getCurrentUsername();
            if (username == null || username.isEmpty()) {
                username = iotRepo.findBymacId(macId).map(Iot::getUsername).orElse("unknown");
            }
            String topic = "user/" + username + "/iot/" + macId + "/command";
            String json = objectMapper.writeValueAsString(payload);
            mqttOutputChannel.send(MessageBuilder.withPayload(json.getBytes())
                    .setHeader("mqtt_topic", topic)
                    .build());
            log.info("Đã gửi MQTT lệnh {} tới {}", command, macId);
        } catch (Exception e) {
            log.error("Lỗi gửi MQTT command tới {}", macId, e);
        }

        // Fallback: cũng gửi qua WebSocket cho thiết bị cũ
        legacyWebSocket.sendCommand(macId, command);
    }

    public boolean sendCommandWithAck(String macId, String command, int timeoutSec) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingAcks.put(macId + "_" + command, future);
        sendMqttCommand(macId, command);
        try {
            return future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Không nhận được ACK cho lệnh {} tới {}", command, macId);
            return false;
        }
    }

    public void processAck(String macId, String payload) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(payload);
            String command = node.has("command") ? node.get("command").asText() : "";
            boolean success = node.has("status") && "OK".equals(node.get("status").asText());
            String key = macId + "_" + command;
            CompletableFuture<Boolean> future = pendingAcks.remove(key);
            if (future != null) {
                future.complete(success);
            }
        } catch (Exception e) {
            log.error("Lỗi parse ACK từ {}", macId, e);
        }
    }

    // ==================== Authorization ====================

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Optional<User> getCurrentUser() {
        return userRepo.findByUsername(getCurrentUsername());
    }

    private boolean canAccess(String macId, String username) {
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isEmpty()) return false;
        // Chủ sở hữu hoặc admin luôn có quyền
        if (device.get().getUsername().equals(username)
                || getCurrentUser().map(u -> u.getRole() == 0).orElse(false)) {
            return true;
        }
        // Kiểm tra share với quyền tối thiểu CONTROL
        return shareRepo.findByMacIdAndSharedUsername(macId, username)
                .map(s -> !s.isExpired() && ("CONTROL".equals(s.getPermission()) || "ADMIN".equals(s.getPermission())))
                .orElse(false);
    }

    private boolean canView(String macId, String username) {
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isEmpty()) return false;
        if (device.get().getUsername().equals(username)
                || getCurrentUser().map(u -> u.getRole() == 0).orElse(false)) {
            return true;
        }
        // VIEW hoặc CONTROL hoặc ADMIN đều được xem
        return shareRepo.findByMacIdAndSharedUsername(macId, username)
                .map(s -> !s.isExpired())
                .orElse(false);
    }

    // ==================== Device CRUD ====================

    public ResponseEntity<List<Iot>> getAllDevices() {
        String username = getCurrentUsername();
        Optional<User> user = userRepo.findByUsername(username);
        if (user.isPresent()) {
            if (user.get().getRole() == 0) {
                return ResponseEntity.ok(iotRepo.findAll());
            }
            return ResponseEntity.ok(iotRepo.findIotByUsername(username));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    public ResponseEntity<Iot> getDeviceByMacId(String macId) {
        String username = getCurrentUsername();
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isPresent() && canAccess(macId, username)) {
            return ResponseEntity.ok(device.get());
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<String> addDevice(Iot newDevice) {
        if (iotRepo.existsBymacId(newDevice.getMacId())) {
            return ResponseEntity.badRequest().body("Thiết bị đã tồn tại!");
        }
        newDevice.setUsername(getCurrentUsername());
        iotRepo.save(newDevice);
        return ResponseEntity.ok("Đã thêm thiết bị thành công");
    }

    public ResponseEntity<Iot> updateDevice(Iot newDevice) {
        String username = getCurrentUsername();
        Optional<Iot> existing = iotRepo.findBymacId(newDevice.getMacId());
        if (existing.isPresent() && canAccess(existing.get().getMacId(), username)) {
            Iot device = existing.get();
            device.setName(newDevice.getName());
            device.setWater(newDevice.getWater());
            device.setDo_am(newDevice.getDo_am());
            return ResponseEntity.ok(iotRepo.save(device));
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<String> deleteDevice(String macId) {
        String username = getCurrentUsername();
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isPresent() && canAccess(macId, username)) {
            iotRepo.deleteBymacId(macId);
            shareRepo.deleteByMacId(macId);
            lastSeen.remove(macId);
            return ResponseEntity.ok("Đã xoá thiết bị");
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<String> sendCommand(String macId, String command) {
        return sendCommand(macId, command, null);
    }

    public ResponseEntity<String> sendCommand(String macId, String command, Integer durationSec) {
        String username = getCurrentUsername();
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccess(macId, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Nếu lệnh RESET: backend tự xoá thiết bị, ESP32 chỉ reset local
        if ("RESET".equals(command)) {
            // Xoá dữ liệu thiết bị trong DB
            shareRepo.deleteByMacId(macId);
            scheduleService.deleteByMacId(macId);
            alertRuleRepo.deleteByMacId(macId);
            lastSeen.remove(macId);
            iotRepo.deleteBymacId(macId);

            // Gửi lệnh RESET xuống ESP32 (chỉ để ESP32 reset preferences + restart)
            sendMqttCommand(macId, "RESET");

            // Ghi lịch sử
            WateringHistory history = new WateringHistory();
            history.setMac(macId);
            history.setAction("RESET");
            history.setAuto(0);
            history.setUsername(username);
            history.setTimestamp(LocalDateTime.now());
            historyRepo.save(history);

            return ResponseEntity.ok("Đã reset thiết bị " + macId + " và xoá khỏi hệ thống");
        }

        sendMqttCommand(macId, command, durationSec);

        // Ghi lịch sử
        WateringHistory history = new WateringHistory();
        history.setMac(macId);
        history.setAction(command);
        history.setAuto(0);
        history.setUsername(username);
        history.setTimestamp(LocalDateTime.now());
        if (durationSec != null) history.setDurationSec(durationSec);
        historyRepo.save(history);

        // Nếu có duration, tự động OFF sau đó
        if (durationSec != null && "ON".equals(command)) {
            scheduler.schedule(() -> sendMqttCommand(macId, "OFF"), durationSec, TimeUnit.SECONDS);
        }

        return ResponseEntity.ok("Đã gửi lệnh " + command + " tới " + macId);
    }

    // ==================== Data Access ====================

    public ResponseEntity<List<SensorData>> getDeviceData(String macId) {
        if (canView(macId, getCurrentUsername())) {
            return ResponseEntity.ok(sensorDataRepo.findByMacOrderByTimeDesc(macId));
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<SensorData>> getDeviceDataStats(String macId,
                                                                LocalDateTime from, LocalDateTime to) {
        if (!canView(macId, getCurrentUsername())) {
            return ResponseEntity.notFound().build();
        }
        List<SensorData> data = sensorDataRepo.findByMacAndTimeBetweenOrderByTimeAsc(macId, from, to);
        return ResponseEntity.ok(data);
    }

    public ResponseEntity<List<WateringHistory>> getWateringHistory(String macId) {
        if (!canView(macId, getCurrentUsername())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(historyRepo.findByMacOrderByTimestampDesc(macId));
    }

    public ResponseEntity<List<WateringHistory>> getWateringHistory(String macId, LocalDateTime from, LocalDateTime to) {
        if (!canView(macId, getCurrentUsername())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(historyRepo.findByMacAndTimestampBetweenOrderByTimestampAsc(macId, from, to));
    }

    public float getCurrentHumidity(String macId) throws Exception {
        if (!canView(macId, getCurrentUsername())) {
            throw new Exception("Không có quyền truy cập");
        }
        // Ưu tiên lấy từ lastSeen (dữ liệu gần nhất có trong DB)
        List<SensorData> latest = sensorDataRepo.findByMacOrderByTimeDesc(macId);
        if (!latest.isEmpty()) {
            SensorData last = latest.get(0);
            if (last.getTime().isAfter(LocalDateTime.now().minusSeconds(60))) {
                return last.getDoam();
            }
        }
        // Fallback: request từ thiết bị qua WebSocket
        return legacyWebSocket.requestHumidity(macId);
    }

    // ==================== Device Sharing ====================

    public ResponseEntity<String> shareDevice(String macId, String sharedUsername, String permission) {
        String username = getCurrentUsername();
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isEmpty() || !device.get().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không phải chủ sở hữu");
        }
        if (!userRepo.existsByUsername(sharedUsername)) {
            return ResponseEntity.badRequest().body("User không tồn tại");
        }
        if (shareRepo.existsByMacIdAndSharedUsername(macId, sharedUsername)) {
            return ResponseEntity.badRequest().body("Đã chia sẻ trước đó");
        }
        DeviceShare share = new DeviceShare();
        share.setMacId(macId);
        share.setOwnerUsername(username);
        share.setSharedUsername(sharedUsername);
        share.setPermission(permission);
        shareRepo.save(share);
        return ResponseEntity.ok("Đã chia sẻ thiết bị cho " + sharedUsername);
    }

    public ResponseEntity<String> removeShare(String macId, String sharedUsername) {
        String username = getCurrentUsername();
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isEmpty() || !device.get().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không phải chủ sở hữu");
        }
        shareRepo.deleteByMacIdAndSharedUsername(macId, sharedUsername);
        return ResponseEntity.ok("Đã thu hồi quyền truy cập");
    }

    public ResponseEntity<List<DeviceShare>> getShares(String macId) {
        return ResponseEntity.ok(shareRepo.findByMacId(macId));
    }

    public ResponseEntity<List<Iot>> getSharedDevices() {
        String username = getCurrentUsername();
        List<DeviceShare> shares = shareRepo.findBySharedUsername(username);
        List<Iot> devices = shares.stream()
                .map(s -> iotRepo.findBymacId(s.getMacId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        return ResponseEntity.ok(devices);
    }

    // ==================== Gửi Config xuống ESP32 ====================

    public ResponseEntity<String> sendConfig(String macId, Integer auto, Float threshold) {
        String username = getCurrentUsername();
        Optional<Iot> device = iotRepo.findBymacId(macId);
        if (device.isEmpty()) return ResponseEntity.notFound().build();
        if (!canAccess(macId, username)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        try {
            Map<String, Object> payload = new HashMap<>();
            if (auto != null) payload.put("auto", auto);
            if (threshold != null) payload.put("threshold", threshold);

            String topic = "user/" + username + "/iot/" + macId + "/config";
            String json = objectMapper.writeValueAsString(payload);
            mqttOutputChannel.send(MessageBuilder.withPayload(json.getBytes())
                    .setHeader("mqtt_topic", topic)
                    .build());

            // Cập nhật DB
            if (auto != null) device.get().setWater(auto);
            if (threshold != null) device.get().setDo_am(threshold);
            iotRepo.save(device.get());

            log.info("Đã gửi config tới {}: auto={}, threshold={}", macId, auto, threshold);
            return ResponseEntity.ok("Đã gửi cấu hình thành công");
        } catch (Exception e) {
            log.error("Lỗi gửi config tới {}", macId, e);
            return ResponseEntity.internalServerError().body("Lỗi gửi cấu hình");
        }
    }

    // ==================== Alert Rules ====================

    public ResponseEntity<AlertRule> getAlertRule(String macId) {
        AlertRule rule = alertRuleRepo.findByMacIdAndUsername(macId, getCurrentUsername())
                .orElse(null);
        return ResponseEntity.ok(rule);
    }

    public ResponseEntity<AlertRule> saveAlertRule(AlertRule rule) {
        rule.setUsername(getCurrentUsername());
        return ResponseEntity.ok(alertRuleRepo.save(rule));
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}