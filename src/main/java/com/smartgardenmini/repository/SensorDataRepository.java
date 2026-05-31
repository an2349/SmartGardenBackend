package com.smartgardenmini.repository;

import com.smartgardenmini.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    List<SensorData> findByMacOrderByTimeDesc(String macId);

    List<SensorData> findByMacAndTimeBetweenOrderByTimeAsc(String mac, LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("DELETE FROM SensorData d WHERE d.time < :threshold")
    int deleteByTimeBefore(LocalDateTime threshold);
}