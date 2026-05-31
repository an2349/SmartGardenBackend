package com.smartgardenmini.repository;

import com.smartgardenmini.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByMacId(String macId);
    List<Schedule> findByEnabledTrueAndTimeBetween(LocalTime from, LocalTime to);
    void deleteByMacId(String macId);
}