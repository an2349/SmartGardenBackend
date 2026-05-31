package com.smartgardenmini.service;

import com.smartgardenmini.model.Schedule;
import com.smartgardenmini.model.WateringHistory;
import com.smartgardenmini.repository.IotRepository;
import com.smartgardenmini.repository.ScheduleRepository;
import com.smartgardenmini.repository.WateringHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private final ScheduleRepository scheduleRepo;
    private final DeviceService deviceService;
    private final WateringHistoryRepository historyRepo;

    public ScheduleService(ScheduleRepository scheduleRepo, DeviceService deviceService,
                           WateringHistoryRepository historyRepo) {
        this.scheduleRepo = scheduleRepo;
        this.deviceService = deviceService;
        this.historyRepo = historyRepo;
    }

    /**
     * Kiểm tra lịch tưới mỗi phút.
     * Chạy vào đầu mỗi phút để bắt các lịch có giờ HH:MM:00
     */
    @Scheduled(cron = "0 0/1 * * * *")
    public void checkSchedules() {
        LocalTime now = LocalTime.now();
        // Tìm các lịch enabled, trong khoảng ±30 giây quanh giờ hiện tại
        List<Schedule> schedules = scheduleRepo.findByEnabledTrueAndTimeBetween(
                now.minusSeconds(30), now.plusSeconds(30));

        for (Schedule schedule : schedules) {
            if (!schedule.matchesToday()) continue;

            log.info("Bắt đầu tưới theo lịch: thiết bị={}, duration={}s",
                    schedule.getMacId(), schedule.getDurationSec());

            // Gửi lệnh ON với duration
            deviceService.sendMqttCommand(schedule.getMacId(), "ON", schedule.getDurationSec());

            // Ghi lịch sử
            WateringHistory history = new WateringHistory();
            history.setMac(schedule.getMacId());
            history.setAction("ON");
            history.setAuto(2); // 2 = tưới theo lịch
            history.setUsername("system");
            history.setTimestamp(LocalDateTime.now());
            history.setDurationSec(schedule.getDurationSec());
            historyRepo.save(history);
        }
    }

    // ==================== CRUD ====================

    public List<Schedule> getSchedules(String macId) {
        return scheduleRepo.findByMacId(macId);
    }

    public Schedule createSchedule(Schedule schedule) {
        return scheduleRepo.save(schedule);
    }

    public Schedule updateSchedule(Long id, Schedule newSchedule) {
        return scheduleRepo.findById(id).map(s -> {
            s.setTime(newSchedule.getTime());
            s.setDurationSec(newSchedule.getDurationSec());
            s.setEnabled(newSchedule.isEnabled());
            s.setDayMask(newSchedule.getDayMask());
            return scheduleRepo.save(s);
        }).orElse(null);
    }

    public void deleteSchedule(Long id) {
        scheduleRepo.deleteById(id);
    }

    public void deleteByMacId(String macId) {
        scheduleRepo.deleteByMacId(macId);
    }
}