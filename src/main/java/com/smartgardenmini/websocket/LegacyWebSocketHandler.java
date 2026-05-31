package com.smartgardenmini.websocket;

import com.smartgardenmini.model.Iot;
import com.smartgardenmini.model.SensorData;
import com.smartgardenmini.model.WateringHistory;
import com.smartgardenmini.repository.IotRepository;
import com.smartgardenmini.repository.SensorDataRepository;
import com.smartgardenmini.repository.WateringHistoryRepository;
import com.smartgardenmini.service.DeviceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Legacy WebSocket handler - giữ lại để tương thích với ESP32 firmware cũ.
 * Ưu tiên dùng MQTT cho thiết bị mới.
 */
@Component
public class LegacyWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LegacyWebSocketHandler.class);
    private final IotRepository iotRepo;
    private final SensorDataRepository sensorDataRepo;
    private final WateringHistoryRepository historyRepo;
    private final DeviceService deviceService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Float>> pendingRequests = new ConcurrentHashMap<>();

    public LegacyWebSocketHandler(IotRepository iotRepo, SensorDataRepository sensorDataRepo,
                                  WateringHistoryRepository historyRepo, DeviceService deviceService) {
        this.iotRepo = iotRepo;
        this.sensorDataRepo = sensorDataRepo;
        this.historyRepo = historyRepo;
        this.deviceService = deviceService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(message.getPayload());

            String deviceId = node.get("Mac").asText();
            float doam = (float) node.get("Doam").asDouble();

            Optional<Iot> deviceOpt = iotRepo.findBymacId(deviceId);
            if (deviceOpt.isEmpty()) {
                log.warn("Thiết bị WebSocket lạ: {}", deviceId);
                return;
            }

            sessions.put(deviceId, session);
            deviceService.markOnline(deviceId);

            // Lưu dữ liệu cảm biến
            SensorData data = new SensorData();
            data.setMac(deviceId);
            data.setDoam(doam);
            data.setTime(LocalDateTime.now());
            sensorDataRepo.save(data);

            // Auto-water nếu bật chế độ tự động
            Iot device = deviceOpt.get();
            if (device.getWater() != 0) {
                String command = doam < device.getDo_am() ? "ON" : "OFF";
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("{\"command\":\"" + command + "\"}"));
                }
                WateringHistory history = new WateringHistory();
                history.setMac(deviceId);
                history.setTimestamp(LocalDateTime.now());
                history.setAction(command);
                history.setAuto(1);
                history.setUsername(device.getUsername());
                historyRepo.save(history);
            }

            // Xử lý pending request (xem độ ẩm)
            if (pendingRequests.containsKey(deviceId)) {
                pendingRequests.get(deviceId).complete(doam);
                pendingRequests.remove(deviceId);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket kết nối mới: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        log.info("WebSocket ngắt kết nối: {} - {}", session.getId(), status);
        sessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    public boolean sendCommand(String deviceId, String command) {
        WebSocketSession session = sessions.get(deviceId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage("{\"command\":\"" + command + "\"}"));
                return true;
            } catch (Exception e) {
                log.error("Lỗi gửi lệnh WebSocket tới {}", deviceId, e);
            }
        }
        return false;
    }

    public Float requestHumidity(String macId) throws Exception {
        WebSocketSession session = sessions.get(macId);
        if (session == null || !session.isOpen()) {
            throw new Exception("Thiết bị " + macId + " không kết nối");
        }
        CompletableFuture<Float> future = new CompletableFuture<>();
        pendingRequests.put(macId, future);
        session.sendMessage(new TextMessage("{\"command\":\"GET_DO_AM\"}"));
        return future.get(5, TimeUnit.SECONDS);
    }
}