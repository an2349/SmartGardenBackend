package com.smartgardenmini.controller;

import com.smartgardenmini.model.*;
import com.smartgardenmini.service.DeviceService;
import com.smartgardenmini.service.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/iot")
public class DeviceController {

    private final DeviceService deviceService;
    private final ScheduleService scheduleService;

    public DeviceController(DeviceService deviceService, ScheduleService scheduleService) {
        this.deviceService = deviceService;
        this.scheduleService = scheduleService;
    }

    // ==================== Device CRUD ====================

    @GetMapping("/devices")
    public ResponseEntity<List<Iot>> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Iot> getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id);
    }

    @PostMapping("/add")
    public ResponseEntity<String> addDevice(@RequestBody Iot newDevice) {
        return deviceService.addDevice(newDevice);
    }

    @PostMapping("/fix")
    public ResponseEntity<Iot> updateDevice(@RequestBody Iot newDevice) {
        return deviceService.updateDevice(newDevice);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<String> deleteDevice(@PathVariable String deviceId) {
        return deviceService.deleteDevice(deviceId);
    }

    // ==================== Control ====================

    @PostMapping("/control/bom/{deviceId}")
    public ResponseEntity<String> controlDevice(
            @PathVariable String deviceId,
            @RequestParam String command,
            @RequestParam(required = false) Integer duration) {
        if (duration != null && duration > 0) {
            return deviceService.sendCommand(deviceId, command, duration);
        }
        return deviceService.sendCommand(deviceId, command);
    }

    // ==================== Sensor Data ====================

    @GetMapping("/data/{deviceId}")
    public ResponseEntity<List<SensorData>> getDeviceData(@PathVariable String deviceId) {
        return deviceService.getDeviceData(deviceId);
    }

    @GetMapping("/data/{deviceId}/stats")
    public ResponseEntity<Map<String, Object>> getDeviceDataStats(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return deviceService.getDeviceDataStats(deviceId, from, to);
    }

    @GetMapping("/doam/{deviceId}")
    public ResponseEntity<Float> getCurrentHumidity(@PathVariable String deviceId) {
        try {
            float doam = deviceService.getCurrentHumidity(deviceId);
            return ResponseEntity.ok(doam);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== Watering History ====================

    @GetMapping("/history/{deviceId}")
    public ResponseEntity<List<WateringHistory>> getWateringHistory(@PathVariable String deviceId) {
        return deviceService.getWateringHistory(deviceId);
    }

    @GetMapping("/history/{deviceId}/range")
    public ResponseEntity<List<WateringHistory>> getWateringHistoryByRange(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return deviceService.getWateringHistory(deviceId, from, to);
    }

    // ==================== Schedules ====================

    @GetMapping("/schedule/{deviceId}")
    public ResponseEntity<List<Schedule>> getSchedules(@PathVariable String deviceId) {
        return ResponseEntity.ok(scheduleService.getSchedules(deviceId));
    }

    @PostMapping("/schedule")
    public ResponseEntity<Schedule> createSchedule(@RequestBody Schedule schedule) {
        Schedule created = scheduleService.createSchedule(schedule);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/schedule/{id}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        Schedule updated = scheduleService.updateSchedule(id, schedule);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/schedule/{id}")
    public ResponseEntity<String> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok("Đã xoá lịch tưới");
    }

    // ==================== Device Sharing ====================

    @PostMapping("/share/{deviceId}")
    public ResponseEntity<String> shareDevice(
            @PathVariable String deviceId,
            @RequestParam String username,
            @RequestParam(defaultValue = "CONTROL") String permission) {
        return deviceService.shareDevice(deviceId, username, permission);
    }

    @DeleteMapping("/share/{deviceId}/{username}")
    public ResponseEntity<String> removeShare(
            @PathVariable String deviceId,
            @PathVariable String username) {
        return deviceService.removeShare(deviceId, username);
    }

    @GetMapping("/share/{deviceId}")
    public ResponseEntity<List<DeviceShare>> getShares(@PathVariable String deviceId) {
        return deviceService.getShares(deviceId);
    }

    @GetMapping("/shared")
    public ResponseEntity<List<Iot>> getSharedDevices() {
        return deviceService.getSharedDevices();
    }

    // ==================== Config (gửi xuống ESP32) ====================

    @PostMapping("/config/{deviceId}")
    public ResponseEntity<String> sendConfig(
            @PathVariable String deviceId,
            @RequestParam(required = false) Integer auto,
            @RequestParam(required = false) Float threshold) {
        if (auto == null && threshold == null) {
            return ResponseEntity.badRequest().body("Phải gửi ít nhất auto hoặc threshold");
        }
        return deviceService.sendConfig(deviceId, auto, threshold);
    }

    // ==================== Alert Rules ====================

    @GetMapping("/alert/{deviceId}")
    public ResponseEntity<AlertRule> getAlertRule(@PathVariable String deviceId) {
        return deviceService.getAlertRule(deviceId);
    }

    @PostMapping("/alert")
    public ResponseEntity<AlertRule> saveAlertRule(@RequestBody AlertRule rule) {
        return deviceService.saveAlertRule(rule);
    }

    // ==================== Specific Sensor Data ====================

    @GetMapping("/sensor/{deviceId}/doam")
    public ResponseEntity<Map<String, Object>> getLatestHumidity(@PathVariable String deviceId) {
        try {
            float doam = deviceService.getCurrentHumidity(deviceId);
            return ResponseEntity.ok(Map.of("macId", deviceId, "doam", doam, "unit", "%"));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sensor/{deviceId}/nhietdo")
    public ResponseEntity<Map<String, Object>> getLatestTemperature(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float nhietDo = data.get(0).getNhietDo();
        if (nhietDo == null) return ResponseEntity.ok(Map.of("macId", deviceId, "message", "Chưa có dữ liệu"));
        return ResponseEntity.ok(Map.of("macId", deviceId, "nhietDo", nhietDo, "unit", "°C"));
    }

    @GetMapping("/sensor/{deviceId}/doamkk")
    public ResponseEntity<Map<String, Object>> getLatestAirHumidity(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float doAmKK = data.get(0).getDoAmKK();
        if (doAmKK == null) return ResponseEntity.ok(Map.of("macId", deviceId, "message", "Chưa có dữ liệu"));
        return ResponseEntity.ok(Map.of("macId", deviceId, "doAmKK", doAmKK, "unit", "%"));
    }

    @GetMapping("/sensor/{deviceId}/anhsang")
    public ResponseEntity<Map<String, Object>> getLatestLight(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float anhSang = data.get(0).getAnhSang();
        if (anhSang == null) return ResponseEntity.ok(Map.of("macId", deviceId, "message", "Chưa có dữ liệu"));
        return ResponseEntity.ok(Map.of("macId", deviceId, "anhSang", anhSang, "unit", "lux"));
    }

    @GetMapping("/sensor/{deviceId}/luuluong")
    public ResponseEntity<Map<String, Object>> getLatestFlow(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float luuLuong = data.get(0).getLuuLuong();
        if (luuLuong == null) return ResponseEntity.ok(Map.of("macId", deviceId, "message", "Chưa có dữ liệu"));
        return ResponseEntity.ok(Map.of("macId", deviceId, "luuLuong", luuLuong, "unit", "L/phút"));
    }

    // ==================== Device Status ====================

    @GetMapping("/status/{deviceId}")
    public ResponseEntity<Map<String, Object>> getDeviceStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(Map.of(
                "online", deviceService.isOnline(deviceId),
                "macId", deviceId
        ));
    }
}