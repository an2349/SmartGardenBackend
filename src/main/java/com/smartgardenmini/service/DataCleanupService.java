package com.smartgardenmini.service;

import com.smartgardenmini.repository.SensorDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DataCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DataCleanupService.class);
    private final SensorDataRepository sensorDataRepo;

    public DataCleanupService(SensorDataRepository sensorDataRepo) {
        this.sensorDataRepo = sensorDataRepo;
    }

    /**
     * Xoá dữ liệu cảm biến cũ hơn 30 ngày, chạy lúc 3h sáng mỗi ngày.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldSensorData() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = sensorDataRepo.deleteByTimeBefore(threshold);
        if (deleted > 0) {
            log.info("Đã dọn dẹp {} bản ghi dữ liệu cảm biến cũ hơn {}", deleted, threshold);
        }
    }
}