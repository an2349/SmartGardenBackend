package com.smartgardenmini.service;

import com.smartgardenmini.model.*;
import com.smartgardenmini.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MqttHandlerService {

    private static final Logger log = LoggerFactory.getLogger(MqttHandlerService.class);
    private final IotRepository iotRepo;
    private final SensorDataRepository sensorDataRepo;
    private final WateringHistoryRepository historyRepo;
    private final DeviceService deviceService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    public MqttHandlerService(IotRepository iotRepo, SensorDataRepository sensorDataRepo,
                              WateringHistoryRepository historyRepo, DeviceService deviceService,
                              AlertService alertService, ObjectMapper objectMapper) {
        this.iotRepo = iotRepo;
        this.sensorDataRepo = sensorDataRepo;
        this.historyRepo = historyRepo;
        this.deviceService = deviceService;
        this.alertService = alertService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handleMessage(String topic, String payload) {
        try {
            // Topic mới: user/{username}/iot/{macId}/{type}
            // Topic cũ (fallback): iot/{macId}/{type}
            String[] parts = topic.split("/");
            String username = null;
            String macId;

            if (parts.length == 5 && "user".equals(parts[0])) {
                // user/{username}/iot/{macId}/{type}
                username = parts[1];
                macId = parts[3];
            } else if (parts.length == 3 && "iot".equals(parts[0])) {
                // iot/{macId}/{type} (fallback cho thiết bị cũ)
                macId = parts[1];
            } else {
                log.warn("MQTT topic không đúng định dạng: {}", topic);
                return;
            }

            if (topic.endsWith("/telemetry")) {
                handleTelemetry(topic, payload, macId, username);
            } else if (topic.endsWith("/status")) {
                handleStatus(topic, payload, macId, username);
            } else if (topic.endsWith("/ack")) {
                handleAck(topic, payload, macId, username);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý MQTT topic={} payload={}", topic, payload, e);
        }
    }

    private void handleTelemetry(String topic, String payload, String macId, String username) throws Exception {
        JsonNode node = objectMapper.readTree(payload);

        log.info("=== NHAN TELEMETRY ===");
        log.info("Topic: {}", topic);
        log.info("Payload: {}", payload);
        log.info("macId={}, username={}", macId, username);

        // Kiem tra thiet bi da dang ky chua
        Iot device = iotRepo.findBymacId(macId).orElse(null);
        if (device == null) {
            log.warn("MQTT telemetry tu thiet bi LA (khong co trong DB): macId={}, username={}", macId, username);
            log.warn("Cac thiet bi trong DB hien co:");
            iotRepo.findAll().forEach(d -> log.warn("  - macId={}, username={}, name={}", d.getMacId(), d.getUsername(), d.getName()));
            return;
        }
        log.info("Thiet bi ton tai trong DB: name={}, username={}", device.getName(), device.getUsername());

        // Danh dau online
        deviceService.markOnline(macId);

        // Luu du lieu cam bien
        SensorData data = new SensorData();
        data.setMac(macId);
        float doam = (float) node.get("doam").asDouble();
        data.setDoam(doam);
        log.info("doam={}", doam);
        if (node.has("nhietDo")) {
            float nhietDo = (float) node.get("nhietDo").asDouble();
            data.setNhietDo(nhietDo);
            log.info("nhietDo={}", nhietDo);
        }
        if (node.has("doAmKK")) {
            float doAmKK = (float) node.get("doAmKK").asDouble();
            data.setDoAmKK(doAmKK);
            log.info("doAmKK={}", doAmKK);
        }
        if (node.has("anhSang")) {
            float anhSang = (float) node.get("anhSang").asDouble();
            data.setAnhSang(anhSang);
            log.info("anhSang={}", anhSang);
        }
        if (node.has("luuLuong")) {
            float luuLuong = (float) node.get("luuLuong").asDouble();
            data.setLuuLuong(luuLuong);
            log.info("luuLuong={}", luuLuong);
        }
        data.setTime(LocalDateTime.now());
        sensorDataRepo.save(data);
        log.info("Da luu SensorData vao DB thanh cong");

        // Cap nhat config tu ESP32 (de dashboard hien thi dung)
        if (node.has("auto")) {
            int auto = node.get("auto").asInt();
            device.setWater(auto);
            log.info("Cap nhat auto={} cho device", auto);
        }
        if (node.has("threshold")) {
            float threshold = (float) node.get("threshold").asDouble();
            device.setDo_am(threshold);
            log.info("Cap nhat threshold={} cho device", threshold);
        }
        iotRepo.save(device);
    }

    private void handleStatus(String topic, String payload, String macId, String username) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        boolean online = node.has("online") && node.get("online").asBoolean();
        deviceService.markOnline(macId);
        log.debug("Thiết bị {} (user={}) online={}", macId, username, online);
    }

    private void handleAck(String topic, String payload, String macId, String username) throws Exception {
        log.debug("ACK từ {} (user={}): {}", macId, username, payload);
        deviceService.processAck(macId, payload);
    }
}