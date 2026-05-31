package com.smartgardenmini.repository;

import com.smartgardenmini.model.DeviceShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceShareRepository extends JpaRepository<DeviceShare, Long> {
    List<DeviceShare> findBySharedUsername(String sharedUsername);
    List<DeviceShare> findByMacId(String macId);
    Optional<DeviceShare> findByMacIdAndSharedUsername(String macId, String sharedUsername);
    boolean existsByMacIdAndSharedUsername(String macId, String sharedUsername);
    void deleteByMacIdAndSharedUsername(String macId, String sharedUsername);
    void deleteByMacId(String macId);
}