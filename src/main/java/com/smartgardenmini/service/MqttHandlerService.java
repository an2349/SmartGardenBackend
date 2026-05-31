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

    public MqttHandlerService(IotRepository iotRepo, SensorDataRepository sensorDataRepo,
                              WateringHistoryRepository historyRepo, DeviceService deviceService,
                              AlertService alertService) {
        this.iotRepo = iotRepo;
        this.sensorDataRepo = sensorDataRepo;
        this.historyRepo = historyRepo;
        this.deviceService = deviceService;
        this.alertService = alertService;
    }

    @Transactional
    public void handleMessage(String topic, String payload) {
        try {
            if (topic.contains("/telemetry")) {
                handleTelemetry(topic, payload);
            } else if (topic.contains("/status")) {
                handleStatus(topic, payload);
            } else if (topic.contains("/ack")) {
                handleAck(topic, payload);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý MQTT topic={} payload={}", topic, payload, e);
        }
    }

    private void handleTelemetry(String topic, String payload) throws Exception {
        // topic: iot/{macId}/telemetry
        String macId = topic.split("/")[1];
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(payload);

        // Kiểm tra thiết bị đã đăng ký chưa
        Iot device = iotRepo.findBymacId(macId).orElse(null);
        if (device == null) {
            log.warn("MQTT telemetry từ thiết bị lạ: {}", macId);
            return;
        }

        // Đánh dấu online
        deviceService.markOnline(macId);

        // Lưu dữ liệu cảm biến
        SensorData data = new SensorData();
        data.setMac(macId);
        data.setDoam((float) node.get("doam").asDouble());
        if (node.has("nhietDo")) data.setNhietDo((float) node.get("nhietDo").asDouble());
        if (node.has("doAmKK")) data.setDoAmKK((float) node.get("doAmKK").asDouble());
        if (node.has("anhSang")) data.setAnhSang((float) node.get("anhSang").asDouble());
        if (node.has("luuLuong")) data.setLuuLuong((float) node.get("luuLuong").asDouble());
        data.setTime(LocalDateTime.now());
        sensorDataRepo.save(data);

        // Cập nhật config từ ESP32 (để dashboard hiển thị đúng)
        if (node.has("auto")) device.setWater(node.get("auto").asInt());
        if (node.has("threshold")) device.setDo_am((float) node.get("threshold").asDouble());
        // Lưu bom vào DB để dashboard biết trạng thái bơm (mở rộng: thêm field relayState)
        iotRepo.save(device);
    }

    private void handleStatus(String topic, String payload) throws Exception {
        String macId = topic.split("/")[1];
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(payload);
        boolean online = node.has("online") && node.get("online").asBoolean();
        deviceService.markOnline(macId);
        log.debug("Thiết bị {} online={}", macId, online);
    }

    private void handleAck(String topic, String payload) throws Exception {
        String macId = topic.split("/")[1];
        log.debug("ACK từ {}: {}", macId, payload);
        deviceService.processAck(macId, payload);
    }
}