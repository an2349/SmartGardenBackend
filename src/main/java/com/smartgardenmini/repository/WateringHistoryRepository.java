package com.smartgardenmini.repository;

import com.smartgardenmini.model.WateringHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WateringHistoryRepository extends JpaRepository<WateringHistory, Long> {
    List<WateringHistory> findByMacOrderByTimestampDesc(String macId);
    List<WateringHistory> findByMacAndTimestampBetweenOrderByTimestampAsc(String mac, LocalDateTime from, LocalDateTime to);
}