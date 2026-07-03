package com.smartgardenmini.controller;

import com.smartgardenmini.dto.*;
import com.smartgardenmini.model.*;
import com.smartgardenmini.service.DeviceService;
import com.smartgardenmini.service.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/iot")
public class DeviceController {

    private final DeviceService deviceService;
    private final ScheduleService scheduleService;

    public DeviceController(DeviceService deviceService, ScheduleService scheduleService) {
        this.deviceService = deviceService;
        this.scheduleService = scheduleService;
    }

    // ==================== Mappers ====================

    private DeviceDTO toDeviceDTO(Iot entity) {
        DeviceDTO dto = new DeviceDTO();
        dto.setMacId(entity.getMacId());
        dto.setName(entity.getName());
        dto.setOnline(entity.isOnline());
        dto.setAutoMode(entity.getWater());
        dto.setThreshold(entity.getDo_am());
        return dto;
    }

    private SensorDataDTO toSensorDataDTO(SensorData entity) {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setMac(entity.getMac());
        dto.setDoam(entity.getDoam());
        dto.setNhietDo(entity.getNhietDo());
        dto.setDoAmKK(entity.getDoAmKK());
        dto.setAnhSang(entity.getAnhSang());
        dto.setLuuLuong(entity.getLuuLuong());
        dto.setTime(entity.getTime());
        return dto;
    }

    private WateringHistoryDTO toWateringHistoryDTO(WateringHistory entity) {
        WateringHistoryDTO dto = new WateringHistoryDTO();
        dto.setMac(entity.getMac());
        dto.setAction(entity.getAction());
        dto.setAuto(entity.getAuto());
        dto.setTimestamp(entity.getTimestamp());
        dto.setDurationSec(entity.getDurationSec());
        return dto;
    }

    private ScheduleDTO toScheduleDTO(Schedule entity) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setId(entity.getId());
        dto.setMacId(entity.getMacId());
        dto.setTime(entity.getTime());
        dto.setDurationSec(entity.getDurationSec());
        dto.setEnabled(entity.isEnabled());
        dto.setDayMask(entity.getDayMask());
        return dto;
    }

    private AlertRuleDTO toAlertRuleDTO(AlertRule entity) {
        AlertRuleDTO dto = new AlertRuleDTO();
        dto.setMacId(entity.getMacId());
        dto.setMinHumidity(entity.getMinHumidity());
        dto.setMaxHumidity(entity.getMaxHumidity());
        dto.setNotifyOnDisconnect(entity.isNotifyOnDisconnect());
        dto.setNotifyOnAutoWater(entity.isNotifyOnAutoWater());
        dto.setEnabled(entity.isEnabled());
        return dto;
    }

    private DeviceShareDTO toDeviceShareDTO(DeviceShare entity) {
        DeviceShareDTO dto = new DeviceShareDTO();
        dto.setId(entity.getId());
        dto.setMacId(entity.getMacId());
        dto.setOwnerUsername(entity.getOwnerUsername());
        dto.setSharedUsername(entity.getSharedUsername());
        dto.setPermission(entity.getPermission());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // ==================== Device CRUD ====================

    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getAllDevices() {
        var result = deviceService.getAllDevices();
        if (result.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<DeviceDTO> dtos = result.getBody().stream().map(this::toDeviceDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @GetMapping("/{macId}")
    public ResponseEntity<ApiResponse<DeviceDTO>> getDeviceById(@PathVariable String macId) {
        var result = deviceService.getDeviceByMacId(macId);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok("Thành công", toDeviceDTO(result.getBody())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeviceDTO>> addDevice(@RequestBody Iot newDevice) {
        var result = deviceService.addDevice(newDevice);
        if (result.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiết bị đã tồn tại!"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm thiết bị thành công", toDeviceDTO(newDevice)));
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceDTO>> updateDevice(@PathVariable String deviceId, @RequestBody Iot newDevice) {
        newDevice.setMacId(deviceId);
        var result = deviceService.updateDevice(newDevice);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật", toDeviceDTO(result.getBody())));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<String>> deleteDevice(@PathVariable String deviceId) {
        var result = deviceService.deleteDevice(deviceId);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(result.getBody()));
    }

    // ==================== Control ====================

    @PostMapping("/control/bom/{deviceId}")
    public ResponseEntity<ApiResponse<String>> controlDevice(
            @PathVariable String deviceId,
            @RequestParam String command,
            @RequestParam(required = false) Integer duration) {
        ResponseEntity<String> result;
        if (duration != null && duration > 0) {
            result = deviceService.sendCommand(deviceId, command, duration);
        } else {
            result = deviceService.sendCommand(deviceId, command);
        }
        if (result.getStatusCode() == HttpStatus.FORBIDDEN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Không có quyền"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.getBody()));
    }

    // ==================== Sensor Data ====================

    @GetMapping("/data/{deviceId}")
    public ResponseEntity<ApiResponse<List<SensorDataDTO>>> getDeviceData(@PathVariable String deviceId) {
        var result = deviceService.getDeviceData(deviceId);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        List<SensorDataDTO> dtos = result.getBody().stream().map(this::toSensorDataDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @GetMapping("/data/{deviceId}/stats")
    public ResponseEntity<ApiResponse<List<SensorDataDTO>>> getDeviceDataStats(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
            var result = deviceService.getDeviceDataStats(deviceId, from, to);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        List<SensorDataDTO> dtos = result.getBody().stream().map(this::toSensorDataDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    // ==================== Watering History ====================

    @GetMapping("/history/{deviceId}")
    public ResponseEntity<ApiResponse<List<WateringHistoryDTO>>> getWateringHistory(@PathVariable String deviceId) {
        var result = deviceService.getWateringHistory(deviceId);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        List<WateringHistoryDTO> dtos = result.getBody().stream().map(this::toWateringHistoryDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @GetMapping("/history/{deviceId}/range")
    public ResponseEntity<ApiResponse<List<WateringHistoryDTO>>> getWateringHistoryByRange(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var result = deviceService.getWateringHistory(deviceId, from, to);
        if (result.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        List<WateringHistoryDTO> dtos = result.getBody().stream().map(this::toWateringHistoryDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    // ==================== Schedules ====================

    @GetMapping("/schedule/{deviceId}")
    public ResponseEntity<ApiResponse<List<ScheduleDTO>>> getSchedules(@PathVariable String deviceId) {
        List<ScheduleDTO> dtos = scheduleService.getSchedules(deviceId).stream().map(this::toScheduleDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<ScheduleDTO>> createSchedule(@RequestBody Schedule schedule) {
        Schedule created = scheduleService.createSchedule(schedule);
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo lịch tưới", toScheduleDTO(created)));
    }

    @PutMapping("/schedule/{id}")
    public ResponseEntity<ApiResponse<ScheduleDTO>> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        Schedule updated = scheduleService.updateSchedule(id, schedule);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật lịch tưới", toScheduleDTO(updated)));
    }

    @DeleteMapping("/schedule/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá lịch tưới"));
    }

    // ==================== Device Sharing ====================

    @PostMapping("/share/{deviceId}")
    public ResponseEntity<ApiResponse<String>> shareDevice(
            @PathVariable String deviceId,
            @RequestParam String username,
            @RequestParam(defaultValue = "CONTROL") String permission) {
        var result = deviceService.shareDevice(deviceId, username, permission);
        if (result.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.getBody()));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.getBody()));
    }

    @DeleteMapping("/share/{deviceId}/{username}")
    public ResponseEntity<ApiResponse<String>> removeShare(
            @PathVariable String deviceId,
            @PathVariable String username) {
        var result = deviceService.removeShare(deviceId, username);
        return ResponseEntity.ok(ApiResponse.ok(result.getBody()));
    }

    @GetMapping("/share/{deviceId}")
    public ResponseEntity<ApiResponse<List<DeviceShareDTO>>> getShares(@PathVariable String deviceId) {
        var result = deviceService.getShares(deviceId);
        List<DeviceShareDTO> dtos = result.getBody().stream().map(this::toDeviceShareDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getSharedDevices() {
        var result = deviceService.getSharedDevices();
        List<DeviceDTO> dtos = result.getBody().stream().map(this::toDeviceDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Thành công", dtos));
    }

    // ==================== Config (gửi xuống ESP32) ====================

    @PostMapping("/config/{deviceId}")
    public ResponseEntity<ApiResponse<String>> sendConfig(
            @PathVariable String deviceId,
            @RequestParam(required = false) Integer auto,
            @RequestParam(required = false) Float threshold) {
        if (auto == null && threshold == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Phải gửi ít nhất auto hoặc threshold"));
        }
        var result = deviceService.sendConfig(deviceId, auto, threshold);
        if (result.getStatusCode() == HttpStatus.FORBIDDEN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Không có quyền"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.getBody()));
    }

    // ==================== Alert Rules ====================

    @GetMapping("/alert/{deviceId}")
    public ResponseEntity<ApiResponse<AlertRuleDTO>> getAlertRule(@PathVariable String deviceId) {
        var result = deviceService.getAlertRule(deviceId);
        if (result.getBody() == null) {
            return ResponseEntity.ok(ApiResponse.ok("Chưa có cấu hình cảnh báo", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("Thành công", toAlertRuleDTO(result.getBody())));
    }

    @PostMapping("/alert")
    public ResponseEntity<ApiResponse<AlertRuleDTO>> saveAlertRule(@RequestBody AlertRule rule) {
        var result = deviceService.saveAlertRule(rule);
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu cấu hình cảnh báo", toAlertRuleDTO(result.getBody())));
    }

    // ==================== Specific Sensor Data ====================

    @GetMapping("/sensor/{deviceId}/doam")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestHumidity(@PathVariable String deviceId) {
        try {
            float doam = deviceService.getCurrentHumidity(deviceId);
            return ResponseEntity.ok(ApiResponse.ok("Thành công", Map.of("macId", deviceId, "doam", doam, "unit", "%")));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sensor/{deviceId}/nhietdo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestTemperature(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float nhietDo = data.get(0).getNhietDo();
        if (nhietDo == null) return ResponseEntity.ok(ApiResponse.ok("Chưa có dữ liệu", null));
        return ResponseEntity.ok(ApiResponse.ok("Thành công", Map.of("macId", deviceId, "nhietDo", nhietDo, "unit", "°C")));
    }

    @GetMapping("/sensor/{deviceId}/doamkk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestAirHumidity(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float doAmKK = data.get(0).getDoAmKK();
        if (doAmKK == null) return ResponseEntity.ok(ApiResponse.ok("Chưa có dữ liệu", null));
        return ResponseEntity.ok(ApiResponse.ok("Thành công", Map.of("macId", deviceId, "doAmKK", doAmKK, "unit", "%")));
    }

    @GetMapping("/sensor/{deviceId}/anhsang")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestLight(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float anhSang = data.get(0).getAnhSang();
        if (anhSang == null) return ResponseEntity.ok(ApiResponse.ok("Chưa có dữ liệu", null));
        return ResponseEntity.ok(ApiResponse.ok("Thành công", Map.of("macId", deviceId, "anhSang", anhSang, "unit", "lux")));
    }

    @GetMapping("/sensor/{deviceId}/luuluong")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestFlow(@PathVariable String deviceId) {
        var data = deviceService.getDeviceData(deviceId).getBody();
        if (data == null || data.isEmpty()) return ResponseEntity.notFound().build();
        Float luuLuong = data.get(0).getLuuLuong();
        if (luuLuong == null) return ResponseEntity.ok(ApiResponse.ok("Chưa có dữ liệu", null));
        return ResponseEntity.ok(ApiResponse.ok("Thành công", Map.of("macId", deviceId, "luuLuong", luuLuong, "unit", "L/phút")));
    }

    // ==================== Device Status ====================

    @GetMapping("/status/{deviceId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeviceStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", Map.of(
                "online", deviceService.isOnline(deviceId),
                "macId", deviceId
        )));
    }
}