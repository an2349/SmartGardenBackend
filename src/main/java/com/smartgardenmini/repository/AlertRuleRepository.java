package com.smartgardenmini.repository;

import com.smartgardenmini.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    Optional<AlertRule> findByMacIdAndUsername(String macId, String username);
    List<AlertRule> findByUsername(String username);
    List<AlertRule> findByMacId(String macId);
    void deleteByMacId(String macId);
}